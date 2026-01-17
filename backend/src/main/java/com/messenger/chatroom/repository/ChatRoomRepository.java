package com.messenger.chatroom.repository;

import com.messenger.chatroom.entity.ChatRoom;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface ChatRoomRepository extends R2dbcRepository<ChatRoom, UUID> {

    @Query("""
        SELECT cr.* FROM chat_rooms cr
        INNER JOIN room_members rm ON cr.id = rm.room_id
        WHERE rm.user_id = :userId
        ORDER BY cr.updated_at DESC
    """)
    Flux<ChatRoom> findAllByUserId(UUID userId);
}
