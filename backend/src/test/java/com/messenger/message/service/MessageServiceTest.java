package com.messenger.message.service;

import com.messenger.chatroom.service.ChatRoomService;
import com.messenger.common.exception.BusinessException;
import com.messenger.message.dto.MessageRequest;
import com.messenger.message.entity.Message;
import com.messenger.message.repository.MessageRepository;
import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private MessageService messageService;

    private UUID roomId;
    private UUID senderId;
    private User testUser;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        senderId = UUID.randomUUID();

        testUser = User.builder()
                .id(senderId)
                .username("testuser")
                .displayName("Test User")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        testMessage = Message.builder()
                .id(UUID.randomUUID())
                .roomId(roomId)
                .senderId(senderId)
                .content("Hello, World!")
                .messageType("TEXT")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("should send message successfully when user is a member")
        void shouldSendMessageSuccessfully() {
            MessageRequest request = MessageRequest.builder()
                    .content("Hello, World!")
                    .messageType("TEXT")
                    .build();

            when(chatRoomService.isMember(roomId, senderId)).thenReturn(Mono.just(true));
            when(messageRepository.save(any(Message.class))).thenReturn(Mono.just(testMessage));
            when(userRepository.findById(senderId)).thenReturn(Mono.just(testUser));

            StepVerifier.create(messageService.sendMessage(roomId, senderId, request))
                    .assertNext(response -> {
                        assertThat(response.getContent()).isEqualTo("Hello, World!");
                        assertThat(response.getSender().getDisplayName()).isEqualTo("Test User");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw error when user is not a member")
        void shouldThrowErrorWhenNotMember() {
            MessageRequest request = MessageRequest.builder()
                    .content("Hello!")
                    .build();

            when(chatRoomService.isMember(roomId, senderId)).thenReturn(Mono.just(false));

            StepVerifier.create(messageService.sendMessage(roomId, senderId, request))
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals("NOT_MEMBER"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getMessages")
    class GetMessages {

        @Test
        @DisplayName("should return messages with pagination")
        void shouldReturnMessagesWithPagination() {
            Message message1 = Message.builder()
                    .id(UUID.randomUUID())
                    .roomId(roomId)
                    .senderId(senderId)
                    .content("Message 1")
                    .messageType("TEXT")
                    .createdAt(OffsetDateTime.now().minusMinutes(2))
                    .build();
            Message message2 = Message.builder()
                    .id(UUID.randomUUID())
                    .roomId(roomId)
                    .senderId(senderId)
                    .content("Message 2")
                    .messageType("TEXT")
                    .createdAt(OffsetDateTime.now().minusMinutes(1))
                    .build();

            when(messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, 51))
                    .thenReturn(Flux.just(message2, message1));
            when(userRepository.findById(senderId)).thenReturn(Mono.just(testUser));

            StepVerifier.create(messageService.getMessages(roomId, null, 50))
                    .assertNext(response -> {
                        assertThat(response.getMessages()).hasSize(2);
                        assertThat(response.isHasMore()).isFalse();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty list when no messages")
        void shouldReturnEmptyListWhenNoMessages() {
            when(messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, 51))
                    .thenReturn(Flux.empty());

            StepVerifier.create(messageService.getMessages(roomId, null, 50))
                    .assertNext(response -> {
                        assertThat(response.getMessages()).isEmpty();
                        assertThat(response.isHasMore()).isFalse();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("deleteMessage")
    class DeleteMessage {

        @Test
        @DisplayName("should delete message when user is sender")
        void shouldDeleteMessageWhenSender() {
            when(messageRepository.findById(testMessage.getId())).thenReturn(Mono.just(testMessage));
            when(messageRepository.save(any(Message.class))).thenReturn(Mono.just(testMessage));

            StepVerifier.create(messageService.deleteMessage(testMessage.getId(), senderId))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw error when user is not sender")
        void shouldThrowErrorWhenNotSender() {
            UUID otherUserId = UUID.randomUUID();
            when(messageRepository.findById(testMessage.getId())).thenReturn(Mono.just(testMessage));

            StepVerifier.create(messageService.deleteMessage(testMessage.getId(), otherUserId))
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals("FORBIDDEN"))
                    .verify();
        }

        @Test
        @DisplayName("should throw error when message not found")
        void shouldThrowErrorWhenMessageNotFound() {
            UUID messageId = UUID.randomUUID();
            when(messageRepository.findById(messageId)).thenReturn(Mono.empty());

            StepVerifier.create(messageService.deleteMessage(messageId, senderId))
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals("MESSAGE_NOT_FOUND"))
                    .verify();
        }
    }
}
