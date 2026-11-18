package com.tlim.creature.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatureRequest(
        @NotBlank String name,
        // In-game lowercase name used by the Hunt Analyser; defaults to name in lowercase
        String actualName,
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
        Integer drownResistance
) {}
