package com.tlim.inventory.dto;

import com.tlim.inventory.SaleDecision;

public record ItemDecisionResponse(
        Long itemId,
        String itemName,
        boolean isTaskItem,
        int quantity,
        Integer goalQuantity,
        int surplusQuantity,
        boolean npcBuyable,
        Integer bestNpcPrice,
        Integer marketPrice,
        SaleDecision decision,
        int estimatedValue,
        boolean missingMarketPrice
) {}
