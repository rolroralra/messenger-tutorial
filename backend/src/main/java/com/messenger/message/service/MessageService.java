package com.messenger.message.service;

import com.messenger.chatroom.service.ChatRoomService;
import com.messenger.common.exception.BusinessException;
import com.messenger.message.dto.MessagePageResponse;
import com.messenger.message.dto.MessageRequest;
import com.messenger.message.dto.MessageResponse;
import com.messenger.message.entity.Message;
import com.messenger.message.repository.MessageRepository;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomService chatRoomService;

    private static final int DEFAULT_PAGE_SIZE = 50;

    public Mono<MessageResponse> sendMessage(UUID roomId, UUID senderId, MessageRequest request) {
        return chatRoomService.isMember(roomId, senderId)
                .flatMap(isMember -> {
                    if (!isMember) {
                        return Mono.error(new BusinessException("NOT_MEMBER", "You are not a member of this room"));
                    }

                    Message message = Message.builder()
                            .roomId(roomId)
                            .senderId(senderId)
                            .content(request.getContent())
                            .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                            .createdAt(OffsetDateTime.now())
                            .updatedAt(OffsetDateTime.now())
                            .build();

                    return messageRepository.save(message);
                })
                .flatMap(this::enrichMessageWithSender)
                .doOnSuccess(m -> log.debug("Message sent to room {}: {}", roomId, m.getId()));
    }

    public Mono<MessagePageResponse> getMessages(UUID roomId, UUID cursor, Integer limit) {
        int pageSize = limit != null ? Math.min(limit, 100) : DEFAULT_PAGE_SIZE;

        Mono<List<Message>> messagesMono;
        if (cursor != null) {
            messagesMono = messageRepository.findByRoomIdBeforeCursor(roomId, cursor, pageSize + 1)
                    .collectList();
        } else {
            messagesMono = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageSize + 1)
                    .collectList();
        }

        return messagesMono.flatMap(messages -> {
            boolean hasMore = messages.size() > pageSize;
            List<Message> pageMessages = hasMore
                    ? messages.subList(0, pageSize)
                    : messages;

            // 시간순으로 정렬 (오래된 것이 먼저)
            Collections.reverse(pageMessages);

            return enrichMessagesWithSenders(pageMessages)
                    .map(enrichedMessages -> MessagePageResponse.builder()
                            .messages(enrichedMessages)
                            .nextCursor(hasMore && !pageMessages.isEmpty()
                                    ? pageMessages.get(0).getId()
                                    : null)
                            .hasMore(hasMore)
                            .build());
        });
    }

    public Mono<Void> deleteMessage(UUID messageId, UUID userId) {
        return messageRepository.findById(messageId)
                .switchIfEmpty(Mono.error(new BusinessException("MESSAGE_NOT_FOUND", "Message not found")))
                .flatMap(message -> {
                    if (!message.getSenderId().equals(userId)) {
                        return Mono.error(new BusinessException("FORBIDDEN", "You can only delete your own messages"));
                    }
                    message.setDeletedAt(OffsetDateTime.now());
                    return messageRepository.save(message);
                })
                .then();
    }

    private Mono<MessageResponse> enrichMessageWithSender(Message message) {
        return userRepository.findById(message.getSenderId())
                .map(user -> {
                    MessageResponse response = MessageResponse.from(message);
                    response.setSender(MessageResponse.SenderInfo.builder()
                            .id(user.getId())
                            .displayName(user.getDisplayName())
                            .avatarUrl(user.getAvatarUrl())
                            .build());
                    return response;
                })
                .defaultIfEmpty(MessageResponse.from(message));
    }

    private Mono<List<MessageResponse>> enrichMessagesWithSenders(List<Message> messages) {
        if (messages.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        return Mono.zip(
                messages.stream()
                        .map(this::enrichMessageWithSender)
                        .toList(),
                objects -> {
                    @SuppressWarnings("unchecked")
                    List<MessageResponse> result = new java.util.ArrayList<>();
                    for (Object obj : objects) {
                        result.add((MessageResponse) obj);
                    }
                    return result;
                }
        );
    }
}
