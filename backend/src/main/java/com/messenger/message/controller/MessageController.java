package com.messenger.message.controller;

import com.messenger.message.dto.MessagePageResponse;
import com.messenger.message.dto.MessageRequest;
import com.messenger.message.dto.MessageResponse;
import com.messenger.message.service.MessageService;
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
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/rooms/{roomId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MessageResponse> sendMessage(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal User user,
            @RequestBody MessageRequest request) {
        return messageService.sendMessage(roomId, user.getId(), request);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public Mono<MessagePageResponse> getMessages(
            @PathVariable UUID roomId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false) Integer limit) {
        return messageService.getMessages(roomId, cursor, limit);
    }

    @DeleteMapping("/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMessage(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal User user) {
        return messageService.deleteMessage(messageId, user.getId());
    }
}
