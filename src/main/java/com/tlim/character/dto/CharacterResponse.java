package com.tlim.character.dto;

import com.tlim.character.Vocation;

import java.time.Instant;

public record CharacterResponse(
        Long id,
        String name,
        Long userId,
        Long serverId,
        String serverName,
        Vocation vocation,
        Instant createdAt
) {}
