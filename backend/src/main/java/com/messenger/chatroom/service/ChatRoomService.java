package com.messenger.chatroom.service;

import com.messenger.chatroom.dto.ChatRoomRequest;
import com.messenger.chatroom.dto.ChatRoomResponse;
import com.messenger.chatroom.dto.RoomMemberResponse;
import com.messenger.chatroom.entity.ChatRoom;
import com.messenger.chatroom.entity.RoomMember;
import com.messenger.chatroom.repository.ChatRoomRepository;
import com.messenger.chatroom.repository.RoomMemberRepository;
import com.messenger.common.exception.BusinessException;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public Mono<ChatRoomResponse> createRoom(UUID creatorId, ChatRoomRequest request) {
        ChatRoom room = ChatRoom.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType() != null ? request.getType() : "GROUP")
                .createdBy(creatorId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return chatRoomRepository.save(room)
                .flatMap(savedRoom -> {
                    // 생성자를 OWNER로 추가
                    RoomMember ownerMember = RoomMember.builder()
                            .roomId(savedRoom.getId())
                            .userId(creatorId)
                            .role("OWNER")
                            .joinedAt(OffsetDateTime.now())
                            .build();

                    return roomMemberRepository.save(ownerMember)
                            .then(addInitialMembers(savedRoom.getId(), request))
                            .then(roomMemberRepository.countByRoomId(savedRoom.getId()))
                            .map(count -> ChatRoomResponse.from(savedRoom, count));
                })
                .doOnSuccess(r -> log.info("Chat room created: {}", r.getName()));
    }

    private Mono<Void> addInitialMembers(UUID roomId, ChatRoomRequest request) {
        if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(request.getMemberIds())
                .flatMap(userId -> addMemberInternal(roomId, userId, "MEMBER"))
                .then();
    }

    public Flux<ChatRoomResponse> getRoomsByUserId(UUID userId) {
        return chatRoomRepository.findAllByUserId(userId)
                .flatMap(room -> roomMemberRepository.countByRoomId(room.getId())
                        .map(count -> ChatRoomResponse.from(room, count)));
    }

    public Mono<ChatRoomResponse> getRoomById(UUID roomId) {
        return chatRoomRepository.findById(roomId)
                .switchIfEmpty(Mono.error(new BusinessException("ROOM_NOT_FOUND", "Chat room not found")))
                .flatMap(room -> roomMemberRepository.countByRoomId(room.getId())
                        .map(count -> ChatRoomResponse.from(room, count)));
    }

    public Mono<ChatRoomResponse> updateRoom(UUID roomId, UUID userId, ChatRoomRequest request) {
        return chatRoomRepository.findById(roomId)
                .switchIfEmpty(Mono.error(new BusinessException("ROOM_NOT_FOUND", "Chat room not found")))
                .flatMap(room -> {
                    if (!room.getCreatedBy().equals(userId)) {
                        return Mono.error(new BusinessException("FORBIDDEN", "Only room creator can update"));
                    }
                    room.setName(request.getName());
                    room.setDescription(request.getDescription());
                    room.setUpdatedAt(OffsetDateTime.now());
                    return chatRoomRepository.save(room);
                })
                .flatMap(room -> roomMemberRepository.countByRoomId(room.getId())
                        .map(count -> ChatRoomResponse.from(room, count)));
    }

    public Mono<Void> deleteRoom(UUID roomId, UUID userId) {
        return chatRoomRepository.findById(roomId)
                .switchIfEmpty(Mono.error(new BusinessException("ROOM_NOT_FOUND", "Chat room not found")))
                .flatMap(room -> {
                    if (!room.getCreatedBy().equals(userId)) {
                        return Mono.error(new BusinessException("FORBIDDEN", "Only room creator can delete"));
                    }
                    return chatRoomRepository.deleteById(roomId);
                });
    }

    public Mono<RoomMemberResponse> addMember(UUID roomId, UUID userId) {
        return roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException("ALREADY_MEMBER", "User is already a member"));
                    }
                    return addMemberInternal(roomId, userId, "MEMBER");
                });
    }

    private Mono<RoomMemberResponse> addMemberInternal(UUID roomId, UUID userId, String role) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new BusinessException("USER_NOT_FOUND", "User not found")))
                .flatMap(user -> {
                    RoomMember member = RoomMember.builder()
                            .roomId(roomId)
                            .userId(userId)
                            .role(role)
                            .joinedAt(OffsetDateTime.now())
                            .build();
                    return roomMemberRepository.save(member)
                            .map(saved -> {
                                RoomMemberResponse response = RoomMemberResponse.from(saved);
                                response.setUsername(user.getUsername());
                                response.setDisplayName(user.getDisplayName());
                                response.setAvatarUrl(user.getAvatarUrl());
                                return response;
                            });
                });
    }

    public Mono<Void> removeMember(UUID roomId, UUID userId) {
        return roomMemberRepository.deleteByRoomIdAndUserId(roomId, userId);
    }

    public Flux<RoomMemberResponse> getMembers(UUID roomId) {
        return roomMemberRepository.findByRoomId(roomId)
                .flatMap(member -> userRepository.findById(member.getUserId())
                        .map(user -> {
                            RoomMemberResponse response = RoomMemberResponse.from(member);
                            response.setUsername(user.getUsername());
                            response.setDisplayName(user.getDisplayName());
                            response.setAvatarUrl(user.getAvatarUrl());
                            return response;
                        }));
    }

    public Mono<Boolean> isMember(UUID roomId, UUID userId) {
        return roomMemberRepository.existsByRoomIdAndUserId(roomId, userId);
    }
}
