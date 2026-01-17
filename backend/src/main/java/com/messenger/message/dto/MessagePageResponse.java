package com.messenger.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessagePageResponse {

    private List<MessageResponse> messages;
    private UUID nextCursor;
    private boolean hasMore;
}
