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
        // Unit price of the recommended channel; 0 when there is nothing to sell or no price
        int sellingPrice,
        // surplusQuantity × sellingPrice
        long estimatedValue,
        boolean missingMarketPrice
) {}
