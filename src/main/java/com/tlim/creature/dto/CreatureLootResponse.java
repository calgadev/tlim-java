package com.tlim.creature.dto;

public record CreatureLootResponse(
        Long id,
        Long itemId,
        String itemName,
        String rarity,
        int minAmount,
        int maxAmount
) {}
