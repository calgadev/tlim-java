package com.tlim.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryRequest(
        @NotNull Long itemId,
        @Min(0) int currentQuantity,
        @Min(0) int targetQuantity
) {}
