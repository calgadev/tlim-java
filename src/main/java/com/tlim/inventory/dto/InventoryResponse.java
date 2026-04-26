package com.tlim.inventory.dto;

import java.time.Instant;

public record InventoryResponse(
        Long id,
        Long characterId,
        Long itemId,
        String itemName,
        int currentQuantity,
        int targetQuantity,
        int deficit,
        int sellableQuantity,
        Instant updatedAt
) {}
