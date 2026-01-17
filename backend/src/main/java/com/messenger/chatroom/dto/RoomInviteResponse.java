package com.messenger.chatroom.dto;

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
public class RoomInviteResponse {

    private UUID roomId;
    private String roomName;
    private String inviteCode;
    private String inviteUrl;
    private OffsetDateTime createdAt;
}
