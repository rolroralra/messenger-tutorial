package com.messenger.auth.service;

import com.messenger.auth.dto.AuthResponse;
import com.messenger.auth.dto.GoogleUserInfo;
import com.messenger.auth.dto.JwtProperties;
import com.messenger.user.dto.UserResponse;
import com.messenger.user.entity.User;
import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String OAUTH_PROVIDER_GOOGLE = "GOOGLE";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Value("${app.oauth.google.redirect-uri:http://localhost:8080/api/v1/auth/callback/google}")
    private String googleRedirectUri;

    public String getGoogleAuthUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + googleClientId +
                "&redirect_uri=" + googleRedirectUri +
                "&response_type=code" +
                "&scope=openid%20profile%20email" +
                "&access_type=offline" +
                "&prompt=consent";
    }

    public Mono<AuthResponse> authenticateWithGoogle(String code) {
        return exchangeCodeForToken(code)
                .flatMap(this::fetchGoogleUserInfo)
                .flatMap(this::findOrCreateUser)
                .map(this::createAuthResponse);
    }

    private Mono<String> exchangeCodeForToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", googleClientId);
        formData.add("client_secret", googleClientSecret);
        formData.add("redirect_uri", googleRedirectUri);
        formData.add("grant_type", "authorization_code");

        return webClient.post()
                .uri(GOOGLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class)
                .map(GoogleTokenResponse::getAccessToken)
                .doOnError(e -> log.error("Failed to exchange code for token", e));
    }

    private Mono<GoogleUserInfo> fetchGoogleUserInfo(String accessToken) {
        return webClient.get()
                .uri(GOOGLE_USERINFO_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(GoogleUserInfo.class)
                .doOnError(e -> log.error("Failed to fetch Google user info", e));
    }

    private Mono<User> findOrCreateUser(GoogleUserInfo googleUserInfo) {
        return userRepository.findByOauthProviderAndOauthId(OAUTH_PROVIDER_GOOGLE, googleUserInfo.getId())
                .switchIfEmpty(userRepository.findByEmail(googleUserInfo.getEmail()))
                .flatMap(existingUser -> updateExistingUser(existingUser, googleUserInfo))
                .switchIfEmpty(createNewUser(googleUserInfo));
    }

    private Mono<User> updateExistingUser(User user, GoogleUserInfo googleUserInfo) {
        // OAuth 정보가 없으면 업데이트
        if (user.getOauthProvider() == null) {
            user.setOauthProvider(OAUTH_PROVIDER_GOOGLE);
            user.setOauthId(googleUserInfo.getId());
        }
        user.setAvatarUrl(googleUserInfo.getPicture());
        user.setDisplayName(googleUserInfo.getName());
        user.setUpdatedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }

    private Mono<User> createNewUser(GoogleUserInfo googleUserInfo) {
        User newUser = User.builder()
                .email(googleUserInfo.getEmail())
                .displayName(googleUserInfo.getName())
                .avatarUrl(googleUserInfo.getPicture())
                .oauthProvider(OAUTH_PROVIDER_GOOGLE)
                .oauthId(googleUserInfo.getId())
                .status("ONLINE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return userRepository.save(newUser);
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiry() / 1000,
                UserResponse.from(user)
        );
    }

    public Mono<AuthResponse> refreshAccessToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            return Mono.error(new IllegalArgumentException("Invalid refresh token"));
        }

        return userRepository.findById(jwtService.extractUserId(refreshToken))
                .map(this::createAuthResponse)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")));
    }

    public Mono<UserResponse> getCurrentUser(String token) {
        if (!jwtService.validateToken(token)) {
            return Mono.error(new IllegalArgumentException("Invalid token"));
        }

        return userRepository.findById(jwtService.extractUserId(token))
                .map(UserResponse::from)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")));
    }

    // Inner class for Google token response
    @lombok.Data
    private static class GoogleTokenResponse {
        private String access_token;
        private String token_type;
        private int expires_in;
        private String refresh_token;
        private String scope;
        private String id_token;

        public String getAccessToken() {
            return access_token;
        }
    }
}
