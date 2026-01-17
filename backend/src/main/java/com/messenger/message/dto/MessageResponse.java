package com.messenger.message.dto;

import com.messenger.message.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private UUID id;
    private UUID roomId;
    private SenderInfo sender;
    private String content;
    private String messageType;
    private OffsetDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderInfo {
        private UUID id;
        private String displayName;
        private String avatarUrl;
    }

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
