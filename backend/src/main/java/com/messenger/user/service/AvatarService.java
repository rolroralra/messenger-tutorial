package com.messenger.user.service;

import com.messenger.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarService {

    private final UserRepository userRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5MB
            .build();

    private static final String AVATAR_CACHE_PREFIX = "avatar:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    public Mono<byte[]> getAvatarByUserId(UUID userId) {
        String cacheKey = AVATAR_CACHE_PREFIX + userId;

        return redisTemplate.opsForValue().get(cacheKey)
                .map(base64 -> Base64.getDecoder().decode(base64))
                .switchIfEmpty(fetchAndCacheAvatar(userId, cacheKey));
    }

    private Mono<byte[]> fetchAndCacheAvatar(UUID userId, String cacheKey) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    String avatarUrl = user.getAvatarUrl();
                    if (avatarUrl == null || avatarUrl.isBlank()) {
                        log.debug("User {} has no avatar URL", userId);
                        return Mono.empty();
                    }

                    log.info("Fetching avatar for user {} from {}", userId, avatarUrl);
                    return fetchImageFromUrl(avatarUrl)
                            .flatMap(imageBytes -> cacheAvatar(cacheKey, imageBytes)
                                    .thenReturn(imageBytes));
                });
    }

    private Mono<byte[]> fetchImageFromUrl(String url) {
        return webClient.get()
                .uri(url)
                .accept(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.IMAGE_GIF)
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnError(e -> log.error("Failed to fetch image from {}: {}", url, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<Boolean> cacheAvatar(String cacheKey, byte[] imageBytes) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return redisTemplate.opsForValue()
                .set(cacheKey, base64, CACHE_TTL)
                .doOnSuccess(success -> log.debug("Cached avatar: {}", cacheKey));
    }

    public Mono<Boolean> invalidateCache(UUID userId) {
        String cacheKey = AVATAR_CACHE_PREFIX + userId;
        return redisTemplate.delete(cacheKey)
                .map(count -> count > 0);
    }
}
