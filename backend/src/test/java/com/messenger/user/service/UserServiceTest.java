package com.messenger.user.service;

import com.messenger.common.exception.BusinessException;
import com.messenger.user.dto.UserRequest;
import com.messenger.user.dto.UserResponse;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .displayName("Test User")
                .avatarUrl("https://example.com/avatar.png")
                .status("OFFLINE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        userRequest = UserRequest.builder()
                .username("testuser")
                .displayName("Test User")
                .avatarUrl("https://example.com/avatar.png")
                .build();
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("should create user successfully when username is unique")
        void shouldCreateUserSuccessfully() {
            when(userRepository.existsByUsername(anyString())).thenReturn(Mono.just(false));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

            StepVerifier.create(userService.createUser(userRequest))
                    .assertNext(response -> {
                        assertThat(response.getUsername()).isEqualTo("testuser");
                        assertThat(response.getDisplayName()).isEqualTo("Test User");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw error when username already exists")
        void shouldThrowErrorWhenUsernameExists() {
            when(userRepository.existsByUsername(anyString())).thenReturn(Mono.just(true));

            StepVerifier.create(userService.createUser(userRequest))
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals("USERNAME_ALREADY_EXISTS"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(testUser.getId())).thenReturn(Mono.just(testUser));

            StepVerifier.create(userService.getUserById(testUser.getId()))
                    .assertNext(response -> {
                        assertThat(response.getId()).isEqualTo(testUser.getId());
                        assertThat(response.getUsername()).isEqualTo("testuser");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should throw error when user not found")
        void shouldThrowErrorWhenNotFound() {
            UUID randomId = UUID.randomUUID();
            when(userRepository.findById(randomId)).thenReturn(Mono.empty());

            StepVerifier.create(userService.getUserById(randomId))
                    .expectErrorMatches(throwable ->
                            throwable instanceof BusinessException &&
                                    ((BusinessException) throwable).getCode().equals("USER_NOT_FOUND"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @Test
        @DisplayName("should return matching users")
        void shouldReturnMatchingUsers() {
            User user1 = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser1")
                    .displayName("Test User 1")
                    .build();
            User user2 = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser2")
                    .displayName("Test User 2")
                    .build();

            when(userRepository.findByUsernameContainingIgnoreCase("test"))
                    .thenReturn(Flux.just(user1, user2));

            StepVerifier.create(userService.searchUsers("test"))
                    .expectNextCount(2)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("should update user successfully")
        void shouldUpdateUserSuccessfully() {
            UserRequest updateRequest = UserRequest.builder()
                    .displayName("Updated Name")
                    .avatarUrl("https://example.com/new-avatar.png")
                    .build();

            User updatedUser = User.builder()
                    .id(testUser.getId())
                    .username(testUser.getUsername())
                    .displayName("Updated Name")
                    .avatarUrl("https://example.com/new-avatar.png")
                    .build();

            when(userRepository.findById(testUser.getId())).thenReturn(Mono.just(testUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

            StepVerifier.create(userService.updateUser(testUser.getId(), updateRequest))
                    .assertNext(response -> {
                        assertThat(response.getDisplayName()).isEqualTo("Updated Name");
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("updateUserStatus")
    class UpdateUserStatus {

        @Test
        @DisplayName("should update user status successfully")
        void shouldUpdateStatusSuccessfully() {
            User onlineUser = User.builder()
                    .id(testUser.getId())
                    .username(testUser.getUsername())
                    .displayName(testUser.getDisplayName())
                    .status("ONLINE")
                    .build();

            when(userRepository.findById(testUser.getId())).thenReturn(Mono.just(testUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(onlineUser));

            StepVerifier.create(userService.updateUserStatus(testUser.getId(), "ONLINE"))
                    .assertNext(response -> {
                        assertThat(response.getStatus()).isEqualTo("ONLINE");
                    })
                    .verifyComplete();
        }
    }
}
