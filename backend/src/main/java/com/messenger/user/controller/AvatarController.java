package com.messenger.user.controller;

import com.messenger.user.service.AvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/avatar")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;

    @GetMapping(value = "/{userId}", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE})
    public Mono<ResponseEntity<byte[]>> getAvatar(@PathVariable UUID userId) {
        return avatarService.getAvatarByUserId(userId)
                .map(imageBytes -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(24)).cachePublic())
                        .contentType(MediaType.IMAGE_PNG)
                        .body(imageBytes))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{userId}/cache")
    public Mono<ResponseEntity<Void>> invalidateCache(@PathVariable UUID userId) {
        return avatarService.invalidateCache(userId)
                .map(success -> success
                        ? ResponseEntity.noContent().<Void>build()
                        : ResponseEntity.notFound().<Void>build());
    }
}
