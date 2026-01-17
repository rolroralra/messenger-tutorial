package com.messenger.message.repository;

import com.messenger.message.entity.Message;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface MessageRepository extends R2dbcRepository<Message, UUID> {

    @Query("""
        SELECT * FROM messages
        WHERE room_id = :roomId AND deleted_at IS NULL
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    Flux<Message> findByRoomIdOrderByCreatedAtDesc(UUID roomId, int limit);

    @Query("""
        SELECT * FROM messages
        WHERE room_id = :roomId AND deleted_at IS NULL AND created_at < (
            SELECT created_at FROM messages WHERE id = :cursorId
        )
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    Flux<Message> findByRoomIdBeforeCursor(UUID roomId, UUID cursorId, int limit);
}
