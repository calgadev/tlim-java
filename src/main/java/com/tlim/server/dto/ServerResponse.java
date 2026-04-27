package com.tlim.server.dto;

import com.tlim.server.PvpType;

import java.time.Instant;

public record ServerResponse(
        Long id,
        String name,
        PvpType pvpType,
        Instant createdAt
) {}
