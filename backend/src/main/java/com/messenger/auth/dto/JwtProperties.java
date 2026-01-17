package com.messenger.auth.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "defaultSecretKeyThatShouldBeChangedInProduction12345678";
    private long accessTokenExpiry = 3600000; // 1시간 (밀리초)
    private long refreshTokenExpiry = 604800000; // 7일 (밀리초)
}
