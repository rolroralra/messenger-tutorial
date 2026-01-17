package com.messenger.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {

    private MessageType type;
    private UUID roomId;
    private UUID messageId;
    private String content;
    private SenderInfo sender;
    private OffsetDateTime createdAt;

    // 타이핑 관련
    private Boolean isTyping;
    private List<String> typingUsers;

    // 에러 관련
    private String errorCode;
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderInfo {
        private UUID id;
        private String displayName;
        private String avatarUrl;
    }
}
