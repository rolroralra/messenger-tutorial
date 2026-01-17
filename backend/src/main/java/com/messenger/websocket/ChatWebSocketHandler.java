package com.messenger.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.auth.service.JwtService;
import com.messenger.message.entity.Message;
import com.messenger.message.repository.MessageRepository;
import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import com.messenger.websocket.dto.MessageType;
import com.messenger.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;

    // 세션 관리
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> roomSessionsMap = new ConcurrentHashMap<>();

    // 메시지 브로드캐스트용 Sink
    private final Sinks.Many<com.messenger.websocket.dto.WebSocketMessage> messageSink =
            Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket connected: {}", sessionId);

        // URL 쿼리에서 JWT 토큰 추출 및 검증
        String query = session.getHandshakeInfo().getUri().getQuery();
        String token = extractToken(query);
        UUID userId = null;

        if (token != null && jwtService.validateToken(token)) {
            userId = jwtService.extractUserId(token);
            log.info("WebSocket authenticated user: {}", userId);
        } else {
            // 후방 호환성: userId 파라미터도 지원 (개발 중에만)
            userId = extractUserId(query);
            if (userId != null) {
                log.warn("WebSocket using legacy userId parameter - please migrate to JWT token");
            }
        }

        if (userId != null) {
            sessionUserMap.put(sessionId, userId);
        } else {
            log.warn("WebSocket connection without valid authentication: {}", sessionId);
        }

        Flux<org.springframework.web.reactive.socket.WebSocketMessage> outbound = messageSink.asFlux()
                .filter(msg -> shouldReceiveMessage(sessionId, msg))
                .map(msg -> {
                    try {
                        return session.textMessage(objectMapper.writeValueAsString(msg));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize message", e);
                        return session.textMessage("{}");
                    }
                });

        Mono<Void> output = session.send(outbound);

        Mono<Void> input = session.receive()
                .map(org.springframework.web.reactive.socket.WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> handleMessage(sessionId, payload))
                .doOnError(e -> log.error("WebSocket error: {}", e.getMessage()))
                .doFinally(signal -> {
                    sessions.remove(sessionId);
                    UUID removedUserId = sessionUserMap.remove(sessionId);
                    if (removedUserId != null) {
                        roomSessionsMap.values().forEach(set -> set.remove(sessionId));
                    }
                    log.info("WebSocket disconnected: {}", sessionId);
                })
                .then();

        return Mono.when(input, output);
    }

    private Mono<Void> handleMessage(String sessionId, String payload) {
        log.info("Received message from session {}: {}", sessionId, payload);
        try {
            com.messenger.websocket.dto.WebSocketMessage message =
                    objectMapper.readValue(payload, com.messenger.websocket.dto.WebSocketMessage.class);

            log.info("Parsed message type: {}, roomId: {}", message.getType(), message.getRoomId());

            return switch (message.getType()) {
                case CHAT -> handleChatMessage(sessionId, message);
                case JOIN -> handleJoinRoom(sessionId, message);
                case LEAVE -> handleLeaveRoom(sessionId, message);
                case TYPING -> handleTyping(sessionId, message);
                default -> {
                    log.warn("Unknown message type: {}", message.getType());
                    yield Mono.empty();
                }
            };
        } catch (JsonProcessingException e) {
            log.error("Failed to parse message: {} - payload: {}", e.getMessage(), payload);
            return Mono.empty();
        }
    }

    private Mono<Void> handleChatMessage(String sessionId, com.messenger.websocket.dto.WebSocketMessage message) {
        UUID userId = sessionUserMap.get(sessionId);
        log.info("handleChatMessage - sessionId: {}, userId: {}, roomId: {}, content: {}",
                sessionId, userId, message.getRoomId(), message.getContent());

        if (userId == null || message.getRoomId() == null) {
            log.warn("handleChatMessage - userId or roomId is null");
            return Mono.empty();
        }

        return userRepository.findById(userId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User not found: {}", userId);
                    return Mono.empty();
                }))
                .flatMap(user -> {
                    log.info("Found user: {}", user.getDisplayName());
                    Message dbMessage = Message.builder()
                            .roomId(message.getRoomId())
                            .senderId(userId)
                            .content(message.getContent())
                            .messageType("TEXT")
                            .createdAt(OffsetDateTime.now())
                            .updatedAt(OffsetDateTime.now())
                            .build();

                    return messageRepository.save(dbMessage)
                            .doOnSuccess(saved -> log.info("Message saved: {}", saved.getId()))
                            .doOnError(e -> log.error("Failed to save message: {}", e.getMessage()))
                            .map(saved -> buildChatResponse(saved, user));
                })
                .doOnNext(response -> {
                    log.info("Broadcasting message to room: {}", response.getRoomId());
                    Sinks.EmitResult result = messageSink.tryEmitNext(response);
                    log.info("Emit result: {}", result);
                })
                .then();
    }

    private Mono<Void> handleJoinRoom(String sessionId, com.messenger.websocket.dto.WebSocketMessage message) {
        UUID roomId = message.getRoomId();
        log.info("handleJoinRoom - sessionId: {}, roomId: {}", sessionId, roomId);

        if (roomId == null) {
            log.warn("handleJoinRoom - roomId is null");
            return Mono.empty();
        }

        roomSessionsMap.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.info("Session {} joined room {}. Room now has {} sessions",
                sessionId, roomId, roomSessionsMap.get(roomId).size());

        UUID userId = sessionUserMap.get(sessionId);
        if (userId != null) {
            return userRepository.findById(userId)
                    .doOnNext(user -> {
                        com.messenger.websocket.dto.WebSocketMessage response =
                                com.messenger.websocket.dto.WebSocketMessage.builder()
                                        .type(MessageType.USER_JOINED)
                                        .roomId(roomId)
                                        .sender(com.messenger.websocket.dto.WebSocketMessage.SenderInfo.builder()
                                                .id(user.getId())
                                                .displayName(user.getDisplayName())
                                                .build())
                                        .createdAt(OffsetDateTime.now())
                                        .build();
                        messageSink.tryEmitNext(response);
                    })
                    .then();
        }
        return Mono.empty();
    }

    private Mono<Void> handleLeaveRoom(String sessionId, com.messenger.websocket.dto.WebSocketMessage message) {
        UUID roomId = message.getRoomId();
        if (roomId == null) return Mono.empty();

        Set<String> roomSessions = roomSessionsMap.get(roomId);
        if (roomSessions != null) {
            roomSessions.remove(sessionId);
        }

        UUID userId = sessionUserMap.get(sessionId);
        if (userId != null) {
            com.messenger.websocket.dto.WebSocketMessage response =
                    com.messenger.websocket.dto.WebSocketMessage.builder()
                            .type(MessageType.USER_LEFT)
                            .roomId(roomId)
                            .sender(com.messenger.websocket.dto.WebSocketMessage.SenderInfo.builder()
                                    .id(userId)
                                    .build())
                            .createdAt(OffsetDateTime.now())
                            .build();
            messageSink.tryEmitNext(response);
        }
        return Mono.empty();
    }

    private Mono<Void> handleTyping(String sessionId, com.messenger.websocket.dto.WebSocketMessage message) {
        UUID roomId = message.getRoomId();
        UUID userId = sessionUserMap.get(sessionId);
        if (roomId == null || userId == null) return Mono.empty();

        return userRepository.findById(userId)
                .doOnNext(user -> {
                    com.messenger.websocket.dto.WebSocketMessage response =
                            com.messenger.websocket.dto.WebSocketMessage.builder()
                                    .type(MessageType.TYPING)
                                    .roomId(roomId)
                                    .sender(com.messenger.websocket.dto.WebSocketMessage.SenderInfo.builder()
                                            .id(user.getId())
                                            .displayName(user.getDisplayName())
                                            .build())
                                    .isTyping(message.getIsTyping())
                                    .createdAt(OffsetDateTime.now())
                                    .build();
                    messageSink.tryEmitNext(response);
                })
                .then();
    }

    private com.messenger.websocket.dto.WebSocketMessage buildChatResponse(Message message, User user) {
        return com.messenger.websocket.dto.WebSocketMessage.builder()
                .type(MessageType.CHAT)
                .roomId(message.getRoomId())
                .messageId(message.getId())
                .content(message.getContent())
                .sender(com.messenger.websocket.dto.WebSocketMessage.SenderInfo.builder()
                        .id(user.getId())
                        .displayName(user.getDisplayName())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private boolean shouldReceiveMessage(String sessionId, com.messenger.websocket.dto.WebSocketMessage message) {
        UUID roomId = message.getRoomId();
        if (roomId == null) return false;

        Set<String> roomSessions = roomSessionsMap.get(roomId);
        return roomSessions != null && roomSessions.contains(sessionId);
    }

    private String extractToken(String query) {
        if (query == null) return null;
        try {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    return keyValue[1];
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract token from query: {}", query);
        }
        return null;
    }

    private UUID extractUserId(String query) {
        if (query == null) return null;
        try {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "userId".equals(keyValue[0])) {
                    return UUID.fromString(keyValue[1]);
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract userId from query: {}", query);
        }
        return null;
    }
}
