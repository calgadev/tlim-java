package com.tlim.server.dto;

import java.time.Instant;

public record ServerResponse(
        Long id,
        String name,
        Instant createdAt
) {}
