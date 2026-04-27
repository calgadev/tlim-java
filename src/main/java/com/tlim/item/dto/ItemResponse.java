package com.tlim.item.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ItemResponse(
        Long id,
        String name,
        String wikiUrl,
        String description,
        String imageUrl,
        BigDecimal weight,
        String category,
        boolean isQuestItem,
        boolean isImbuementMaterial,
        boolean isDeliveryItem,
        boolean isTaskItem,
        List<NpcBuyerResponse> npcBuyers,
        Instant updatedAt
) {}
