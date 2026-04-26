package com.tlim.item.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ItemRequest(
        @NotBlank String name,
        String description,
        String imageUrl,
        BigDecimal weight,
        String category,
        boolean isQuestItem,
        boolean isImbuementMaterial,
        boolean isDeliveryItem
) {}
