package com.messenger.chatroom.service;

import com.messenger.chatroom.dto.ChatRoomRequest;
import com.messenger.chatroom.entity.ChatRoom;
import com.messenger.chatroom.entity.RoomMember;
import com.messenger.chatroom.repository.ChatRoomRepository;
import com.messenger.chatroom.repository.RoomMemberRepository;
import com.messenger.common.exception.BusinessException;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private UUID creatorId;
    private UUID roomId;
    private ChatRoom testRoom;
    private User testUser;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        testUser = User.builder()
                .id(creatorId)
                .username("creator")
                .displayName("Room Creator")
                .build();

        testRoom = ChatRoom.builder()
                .id(roomId)
                .name("Test Room")
                .description("Test Description")
                .type("GROUP")
                .createdBy(creatorId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createRoom")
    class CreateRoom {

        @Test
        @DisplayName("should create room successfully")
        void shouldCreateRoomSuccessfully() {
            ChatRoomRequest request = ChatRoomRequest.builder()
                    .name("Test Room")
                    .description("Test Description")
                    .type("GROUP")
                    .build();

            RoomMember ownerMember = RoomMember.builder()
                    .id(UUID.randomUUID())
                    .roomId(roomId)
                    .userId(creatorId)
                    .role("OWNER")
                    .build();

            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(Mono.just(testRoom));
            when(roomMemberRepository.save(any(RoomMember.class))).thenReturn(Mono.just(ownerMember));
            when(roomMemberRepository.countByRoomId(roomId)).thenReturn(Mono.just(1L));

            StepVerifier.create(chatRoomService.createRoom(creatorId, request))
                    .assertNext(response -> {
                        assertThat(response.getName()).isEqualTo("Test Room");
                        assertThat(response.getMemberCount()).isEqualTo(1L);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should create room with initial members")
        void shouldCreateRoomWithInitialMembers() {
            UUID memberId = UUID.randomUUID();
            User memberUser = User.builder()
                    .id(memberId)
                    .username("member")
                    .displayName("Member User")
                    .build();

            ChatRoomRequest request = ChatRoomRequest.builder()
                    .name("Test Room")
                    .type("GROUP")
                    .memberIds(List.of(memberId))
                    .build();

            RoomMember ownerMember = RoomMember.builder()
                    .id(UUID.randomUUID())
                    .roomId(roomId)
                    .userId(creatorId)
                    .role("OWNER")
                    .build();

            RoomMember member = RoomMember.builder()
                    .id(UUID.randomUUID())
                    .roomId(roomId)
                    .userId(memberId)
                    .role("MEMBER")
                    .build();

            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(Mono.just(testRoom));
            when(roomMemberRepository.save(any(RoomMember.class)))
                    .thenReturn(Mono.just(ownerMember))
                    .thenReturn(Mono.just(member));
            when(userRepository.findById(memberId)).thenReturn(Mono.just(memberUser));
            when(roomMemberRepository.countByRoomId(roomId)).thenReturn(Mono.just(2L));

            StepVerifier.create(chatRoomService.createRoom(creatorId, request))
                    .assertNext(response -> {
                        assertThat(response.getName()).isEqualTo("Test Room");
                        assertThat(response.getMemberCount()).isEqualTo(2L);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getRoomById")
    class GetRoomById {

        @Test
        @DisplayName("should return room when found")
        void shouldReturnRoomWhenFound() {
            when(chatRoomRepository.findById(roomId)).thenReturn(Mono.just(testRoom));
            when(roomMemberRepository.countByRoomId(roomId)).thenReturn(Mono.just(3L));

            StepVerifier.create(chatRoomService.getRoomById(roomId))
                    .assertNext(response -> {
                        assertThat(response.getId()).isEqualTo(roomId);
                        assertThat(response.getName()).isEqualTo("Test Room");
                        assertThat(response.getMemberCount()).isEqualTo(3L);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw error when room not found")
        void shouldThrowErrorWhenNotFound() {
            when(chatRoomRepository.findById(roomId)).thenReturn(Mono.empty());

            StepVerifier.create(chatRoomService.getRoomById(roomId))
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals("ROOM_NOT_FOUND"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getRoomsByUserId")
    class GetRoomsByUserId {

        @Test
        @DisplayName("should return user's rooms")
        void shouldReturnUserRooms() {
            ChatRoom room1 = ChatRoom.builder()
                    .id(UUID.randomUUID())
                    .name("Room 1")
                    .createdBy(creatorId)
                    .build();
            ChatRoom room2 = ChatRoom.builder()
                    .id(UUID.randomUUID())
                    .name("Room 2")
                    .createdBy(creatorId)
                    .build();

            when(chatRoomRepository.findAllByUserId(creatorId)).thenReturn(Flux.just(room1, room2));
            when(roomMemberRepository.countByRoomId(any())).thenReturn(Mono.just(2L));

            StepVerifier.create(chatRoomService.getRoomsByUserId(creatorId))
                    .expectNextCount(2)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("addMember")
    class AddMember {

        @Test
        @DisplayName("should add member successfully")
        void shouldAddMemberSuccessfully() {
            UUID newUserId = UUID.randomUUID();
            User newUser = User.builder()
                    .id(newUserId)
                    .username("newuser")
                    .displayName("New User")
                    .build();

            RoomMember newMember = RoomMember.builder()
                    .id(UUID.randomUUID())
                    .roomId(roomId)
                    .userId(newUserId)
                    .role("MEMBER")
                    .build();

            when(roomMemberRepository.existsByRoomIdAndUserId(roomId, newUserId)).thenReturn(Mono.just(false));
            when(userRepository.findById(newUserId)).thenReturn(Mono.just(newUser));
            when(roomMemberRepository.save(any(RoomMember.class))).thenReturn(Mono.just(newMember));

            StepVerifier.create(chatRoomService.addMember(roomId, newUserId))
                    .assertNext(response -> {
                        assertThat(response.getUserId()).isEqualTo(newUserId);
                        assertThat(response.getRole()).isEqualTo("MEMBER");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw error when already a member")
        void shouldThrowErrorWhenAlreadyMember() {
            UUID existingUserId = UUID.randomUUID();
            when(roomMemberRepository.existsByRoomIdAndUserId(roomId, existingUserId)).thenReturn(Mono.just(true));

            StepVerifier.create(chatRoomService.addMember(roomId, existingUserId))
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals("ALREADY_MEMBER"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("deleteRoom")
    class DeleteRoom {

        @Test
        @DisplayName("should delete room when user is creator")
        void shouldDeleteRoomWhenCreator() {
            when(chatRoomRepository.findById(roomId)).thenReturn(Mono.just(testRoom));
            when(chatRoomRepository.deleteById(roomId)).thenReturn(Mono.empty());

            StepVerifier.create(chatRoomService.deleteRoom(roomId, creatorId))
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw error when user is not creator")
        void shouldThrowErrorWhenNotCreator() {
            UUID otherUserId = UUID.randomUUID();
            when(chatRoomRepository.findById(roomId)).thenReturn(Mono.just(testRoom));

            StepVerifier.create(chatRoomService.deleteRoom(roomId, otherUserId))
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals("FORBIDDEN"))
                    .verify();
        }
    }
}
