package com.messenger.chatroom.controller;

import com.messenger.chatroom.dto.ChatRoomRequest;
import com.messenger.chatroom.dto.ChatRoomResponse;
import com.messenger.chatroom.dto.RoomMemberResponse;
import com.messenger.chatroom.service.ChatRoomService;
import com.messenger.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ChatRoomResponse> createRoom(
            @AuthenticationPrincipal User user,
            @RequestBody ChatRoomRequest request) {
        return chatRoomService.createRoom(user.getId(), request);
    }

    @GetMapping
    public Flux<ChatRoomResponse> getRooms(@AuthenticationPrincipal User user) {
        return chatRoomService.getRoomsByUserId(user.getId());
    }

    @GetMapping("/{roomId}")
    public Mono<ChatRoomResponse> getRoom(@PathVariable UUID roomId) {
        return chatRoomService.getRoomById(roomId);
    }

    @PutMapping("/{roomId}")
    public Mono<ChatRoomResponse> updateRoom(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal User user,
            @RequestBody ChatRoomRequest request) {
        return chatRoomService.updateRoom(roomId, user.getId(), request);
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteRoom(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal User user) {
        return chatRoomService.deleteRoom(roomId, user.getId());
    }

    @PostMapping("/{roomId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RoomMemberResponse> addMember(
            @PathVariable UUID roomId,
            @RequestParam UUID userId) {
        return chatRoomService.addMember(roomId, userId);
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeMember(
            @PathVariable UUID roomId,
            @PathVariable UUID userId) {
        return chatRoomService.removeMember(roomId, userId);
    }

    @GetMapping("/{roomId}/members")
    public Flux<RoomMemberResponse> getMembers(@PathVariable UUID roomId) {
        return chatRoomService.getMembers(roomId);
    }
}
