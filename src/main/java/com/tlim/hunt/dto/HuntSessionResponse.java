package com.tlim.hunt.dto;

import java.time.Instant;
import java.util.List;

public record HuntSessionResponse(
        Long id,
        Long characterId,
        String location,
        Instant startedAt,
        Instant endedAt,
        String duration,
        int rawXp,
        int xpWithBonus,
        int lootTotal,
        int supplies,
        int damage,
        int healing,
        List<HuntSessionItemResponse> items,
        List<HuntSessionMonsterResponse> monsters
) {}
