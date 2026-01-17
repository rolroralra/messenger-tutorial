package com.messenger.user.service;

import com.messenger.common.exception.BusinessException;
import com.messenger.user.dto.UserRequest;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Mono<UserResponse> createUser(UserRequest request) {
        return userRepository.existsByUsername(request.getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException("USERNAME_ALREADY_EXISTS", "Username already exists"));
                    }
                    User user = User.builder()
                            .username(request.getUsername())
                            .displayName(request.getDisplayName())
                            .avatarUrl(request.getAvatarUrl())
                            .status("OFFLINE")
                            .createdAt(OffsetDateTime.now())
                            .updatedAt(OffsetDateTime.now())
                            .build();
                    return userRepository.save(user);
                })
                .map(UserResponse::from)
                .doOnSuccess(user -> log.info("User created: {}", user.getUsername()));
    }

    public Mono<UserResponse> getUserById(UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .switchIfEmpty(Mono.error(new BusinessException("USER_NOT_FOUND", "User not found")));
    }

    public Mono<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserResponse::from)
                .switchIfEmpty(Mono.error(new BusinessException("USER_NOT_FOUND", "User not found")));
    }

    public Flux<UserResponse> searchUsers(String query) {
        return userRepository.findByUsernameContainingIgnoreCase(query)
                .map(UserResponse::from);
    }

    public Mono<UserResponse> updateUser(UUID id, UserRequest request) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("USER_NOT_FOUND", "User not found")))
                .flatMap(user -> {
                    user.setDisplayName(request.getDisplayName());
                    if (request.getAvatarUrl() != null) {
                        user.setAvatarUrl(request.getAvatarUrl());
                    }
                    user.setUpdatedAt(OffsetDateTime.now());
                    return userRepository.save(user);
                })
                .map(UserResponse::from);
    }

    public Mono<UserResponse> updateUserStatus(UUID id, String status) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("USER_NOT_FOUND", "User not found")))
                .flatMap(user -> {
                    user.setStatus(status);
                    user.setUpdatedAt(OffsetDateTime.now());
                    return userRepository.save(user);
                })
                .map(UserResponse::from);
    }

    public Mono<Void> deleteUser(UUID id) {
        return userRepository.deleteById(id);
    }
}
