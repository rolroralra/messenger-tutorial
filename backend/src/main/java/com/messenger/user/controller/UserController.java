package com.messenger.user.controller;

import com.messenger.user.dto.UserRequest;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponse> createUser(@RequestBody UserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/{id}")
    public Mono<UserResponse> getUser(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @GetMapping("/username/{username}")
    public Mono<UserResponse> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username);
    }

    @GetMapping("/search")
    public Flux<UserResponse> searchUsers(@RequestParam String q) {
        return userService.searchUsers(q);
    }

    @PutMapping("/{id}")
    public Mono<UserResponse> updateUser(@PathVariable UUID id, @RequestBody UserRequest request) {
        return userService.updateUser(id, request);
    }

    @PatchMapping("/{id}/status")
    public Mono<UserResponse> updateUserStatus(@PathVariable UUID id, @RequestParam String status) {
        return userService.updateUserStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteUser(@PathVariable UUID id) {
        return userService.deleteUser(id);
    }
}
