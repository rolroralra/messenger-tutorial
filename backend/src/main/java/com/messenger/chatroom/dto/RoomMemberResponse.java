package com.messenger.chatroom.dto;

import com.messenger.chatroom.entity.RoomMember;
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
public class RoomMemberResponse {

    private UUID id;
    private UUID roomId;
    private UUID userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String role;
    private OffsetDateTime joinedAt;

    public static RoomMemberResponse from(RoomMember member) {
        return RoomMemberResponse.builder()
                .id(member.getId())
                .roomId(member.getRoomId())
                .userId(member.getUserId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
