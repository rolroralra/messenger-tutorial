package com.messenger.chatroom.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("room_members")
public class RoomMember {

    @Id
    private UUID id;

    @Column("room_id")
    private UUID roomId;

    @Column("user_id")
    private UUID userId;

    @Column("role")
    @Builder.Default
    private String role = "MEMBER";

    @Column("joined_at")
    private OffsetDateTime joinedAt;

    @Column("last_read_at")
    private OffsetDateTime lastReadAt;
}
