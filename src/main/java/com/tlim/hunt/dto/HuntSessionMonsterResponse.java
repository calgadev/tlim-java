package com.tlim.hunt.dto;

public record HuntSessionMonsterResponse(
        Long id,
        Long creatureId,
        String creatureName,
        int killCount
) {}
