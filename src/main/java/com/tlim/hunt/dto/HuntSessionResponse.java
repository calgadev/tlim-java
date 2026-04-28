package com.tlim.hunt.dto;

import java.time.Instant;
import java.util.List;

public record HuntSessionResponse(
        Long id,
        Long characterId,
        String name,
        String location,
        boolean isParty,
        String notes,
        Instant startedAt,
        Instant endedAt,
        String duration,
        int rawXp,
        int xpWithBonus,
        int lootTotal,
        int supplies,
        int damage,
        int healing,
        Integer charLevel,
        Integer allyEkLevel,
        Integer allyMsLevel,
        Integer allyEdLevel,
        Integer allyRpLevel,
        Integer allyEmLevel,
        List<HuntSessionItemResponse> items,
        List<HuntSessionMonsterResponse> monsters,
        List<String> skippedItems,
        List<String> skippedMonsters
) {}
