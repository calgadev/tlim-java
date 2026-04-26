package com.tlim.character.dto;

import java.time.Instant;

public record CharacterResponse(
        Long id,
        String name,
        Long userId,
        Long serverId,
        String serverName,
        Instant createdAt
) {}
