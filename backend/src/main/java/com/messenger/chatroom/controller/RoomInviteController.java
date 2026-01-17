package com.messenger.chatroom.controller;

import com.messenger.chatroom.dto.ChatRoomResponse;
import com.messenger.chatroom.dto.RoomInviteResponse;
import com.messenger.chatroom.service.RoomInviteService;
import com.messenger.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoomInviteController {

    private final RoomInviteService roomInviteService;

    @PostMapping("/rooms/{roomId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RoomInviteResponse> createInvite(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal User user) {
        return roomInviteService.createInvite(roomId, user.getId());
    }

    @GetMapping("/invites/{code}")
    public Mono<RoomInviteResponse> getInviteByCode(@PathVariable String code) {
        return roomInviteService.getInviteByCode(code);
    }

    @PostMapping("/invites/{code}/join")
    public Mono<ChatRoomResponse> joinByInviteCode(
            @PathVariable String code,
            @AuthenticationPrincipal User user) {
        return roomInviteService.joinByInviteCode(code, user.getId());
    }

    @DeleteMapping("/invites/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteInvite(@PathVariable String code) {
        return roomInviteService.deleteInvite(code).then();
    }
}
