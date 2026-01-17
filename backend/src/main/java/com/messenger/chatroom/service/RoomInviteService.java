package com.messenger.chatroom.service;

import com.messenger.chatroom.dto.ChatRoomResponse;
import com.messenger.chatroom.dto.RoomInviteResponse;
import com.messenger.chatroom.entity.RoomMember;
import com.messenger.chatroom.repository.ChatRoomRepository;
import com.messenger.chatroom.repository.RoomMemberRepository;
import com.messenger.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomInviteService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private static final String INVITE_KEY_PREFIX = "invite:";
    private static final Duration INVITE_TTL = Duration.ofDays(7);

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public Mono<RoomInviteResponse> createInvite(UUID roomId, UUID userId) {
        return chatRoomRepository.findById(roomId)
                .switchIfEmpty(Mono.error(new BusinessException("ROOM_NOT_FOUND", "Chat room not found")))
                .flatMap(room -> roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)
                        .flatMap(isMember -> {
                            if (!isMember) {
                                return Mono.error(new BusinessException("NOT_A_MEMBER", "You are not a member of this room"));
                            }

                            String inviteCode = generateInviteCode();
                            String cacheKey = INVITE_KEY_PREFIX + inviteCode;

                            return redisTemplate.opsForValue()
                                    .set(cacheKey, roomId.toString(), INVITE_TTL)
                                    .map(success -> RoomInviteResponse.builder()
                                            .roomId(roomId)
                                            .roomName(room.getName())
                                            .inviteCode(inviteCode)
                                            .inviteUrl(frontendUrl + "/invite/" + inviteCode)
                                            .createdAt(OffsetDateTime.now())
                                            .build());
                        }))
                .doOnSuccess(r -> log.info("Invite created for room: {} with code: {}", roomId, r.getInviteCode()))
                .cache();
    }

    public Mono<RoomInviteResponse> getInviteByCode(String inviteCode) {
        String cacheKey = INVITE_KEY_PREFIX + inviteCode;

        return redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(Mono.error(new BusinessException("INVITE_NOT_FOUND", "Invite code not found or expired")))
                .flatMap(roomIdStr -> {
                    UUID roomId = UUID.fromString(roomIdStr);
                    return chatRoomRepository.findById(roomId)
                            .switchIfEmpty(Mono.error(new BusinessException("ROOM_NOT_FOUND", "Chat room not found")))
                            .map(room -> RoomInviteResponse.builder()
                                    .roomId(roomId)
                                    .roomName(room.getName())
                                    .inviteCode(inviteCode)
                                    .inviteUrl(frontendUrl + "/invite/" + inviteCode)
                                    .build());
                });
    }

    @Transactional
    public Mono<ChatRoomResponse> joinByInviteCode(String inviteCode, UUID userId) {
        String cacheKey = INVITE_KEY_PREFIX + inviteCode;

        return redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(Mono.error(new BusinessException("INVITE_NOT_FOUND", "Invite code not found or expired")))
                .flatMap(roomIdStr -> {
                    UUID roomId = UUID.fromString(roomIdStr);
                    return chatRoomRepository.findById(roomId)
                            .switchIfEmpty(Mono.error(new BusinessException("ROOM_NOT_FOUND", "Chat room not found")))
                            .flatMap(room -> roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)
                                    .flatMap(isMember -> {
                                        if (isMember) {
                                            // Already a member, just return the room info
                                            return roomMemberRepository.countByRoomId(roomId)
                                                    .map(count -> ChatRoomResponse.from(room, count));
                                        }

                                        // Add as new member
                                        RoomMember member = RoomMember.builder()
                                                .roomId(roomId)
                                                .userId(userId)
                                                .role("MEMBER")
                                                .joinedAt(OffsetDateTime.now())
                                                .build();

                                        return roomMemberRepository.save(member)
                                                .then(roomMemberRepository.countByRoomId(roomId))
                                                .map(count -> ChatRoomResponse.from(room, count));
                                    }));
                })
                .doOnSuccess(r -> log.info("User {} joined room {} via invite code", userId, r.getId()));
    }

    public Mono<Boolean> deleteInvite(String inviteCode) {
        String cacheKey = INVITE_KEY_PREFIX + inviteCode;
        return redisTemplate.delete(cacheKey)
                .map(count -> count > 0);
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
