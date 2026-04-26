package com.tlim.creature.dto;

import java.time.Instant;
import java.util.List;

public record CreatureResponse(
        Long id,
        String name,
        String wikiUrl,
        String imageUrl,
        Integer hp,
        Integer experience,
        Integer physicalResistance,
        Integer fireResistance,
        Integer iceResistance,
        Integer energyResistance,
        Integer earthResistance,
        Integer deathResistance,
        Integer holyResistance,
        Integer drownResistance,
        List<CreatureLootResponse> loot,
        Instant updatedAt
) {}
