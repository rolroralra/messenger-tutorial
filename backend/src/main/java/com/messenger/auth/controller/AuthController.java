package com.messenger.auth.controller;

import com.messenger.auth.dto.AuthResponse;
import com.messenger.auth.dto.RefreshTokenRequest;
import com.messenger.auth.service.AuthService;
import com.messenger.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Google OAuth 로그인 URL 반환
     */
    @GetMapping("/google")
    public Mono<ResponseEntity<Map<String, String>>> getGoogleAuthUrl() {
        String authUrl = authService.getGoogleAuthUrl();
        return Mono.just(ResponseEntity.ok(Map.of("authUrl", authUrl)));
    }

    /**
     * Google OAuth 콜백 처리
     * 프론트엔드에서 code를 받아서 처리
     */
    @GetMapping("/callback/google")
    public Mono<ResponseEntity<AuthResponse>> handleGoogleCallback(@RequestParam("code") String code) {
        log.info("Received Google OAuth callback with code");
        return authService.authenticateWithGoogle(code)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Google OAuth authentication failed", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    /**
     * 프론트엔드 콜백용 - 리다이렉트 방식
     * Google에서 직접 리다이렉트되는 경우
     */
    @GetMapping("/oauth2/callback/google")
    public Mono<ResponseEntity<Void>> handleGoogleRedirectCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state
    ) {
        log.info("Received Google OAuth redirect callback");
        return authService.authenticateWithGoogle(code)
                .map(authResponse -> {
                    // 프론트엔드로 리다이렉트하면서 토큰 전달
                    String redirectUrl = String.format(
                            "http://localhost:5173/auth/callback?accessToken=%s&refreshToken=%s",
                            authResponse.getAccessToken(),
                            authResponse.getRefreshToken()
                    );
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(redirectUrl))
                            .<Void>build();
                })
                .onErrorResume(e -> {
                    log.error("Google OAuth redirect callback failed", e);
                    String errorRedirectUrl = "http://localhost:5173/login?error=auth_failed";
                    return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(errorRedirectUrl))
                            .build());
                });
    }

    /**
     * Access Token 갱신
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return authService.refreshAccessToken(request.getRefreshToken())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Token refresh failed", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    /**
     * 현재 로그인된 사용자 정보 조회
     */
    @GetMapping("/me")
    public Mono<ResponseEntity<UserResponse>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        if (token == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return authService.getCurrentUser(token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Failed to get current user", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    /**
     * 로그아웃 (클라이언트에서 토큰 삭제)
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout() {
        // JWT 기반이므로 서버에서 할 작업은 없음
        // 필요시 Redis에 토큰 블랙리스트 추가 가능
        return Mono.just(ResponseEntity.ok(Map.of("message", "Logged out successfully")));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
