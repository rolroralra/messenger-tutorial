package com.messenger.message.entity;

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
@Table("messages")
public class Message {

    @Id
    private UUID id;

    @Column("room_id")
    private UUID roomId;

    @Column("sender_id")
    private UUID senderId;

    @Column("content")
    private String content;

    @Column("message_type")
    @Builder.Default
    private String messageType = "TEXT";

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;

    @Column("deleted_at")
    private OffsetDateTime deletedAt;
}
