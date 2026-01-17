package com.messenger.chatroom.repository;

import com.messenger.chatroom.entity.RoomMember;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface RoomMemberRepository extends R2dbcRepository<RoomMember, UUID> {

    Flux<RoomMember> findByRoomId(UUID roomId);

    Flux<RoomMember> findByUserId(UUID userId);

    Mono<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);

    Mono<Boolean> existsByRoomIdAndUserId(UUID roomId, UUID userId);

    Mono<Void> deleteByRoomIdAndUserId(UUID roomId, UUID userId);

    Mono<Long> countByRoomId(UUID roomId);
}
