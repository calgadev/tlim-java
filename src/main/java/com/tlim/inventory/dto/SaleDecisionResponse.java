package com.tlim.inventory.dto;

import java.util.List;

public record SaleDecisionResponse(
        Long characterId,
        Long serverId,
        List<ItemDecisionResponse> items,
        // Gold obtainable by selling only the surplus above each goal
        long passiveGold,
        // Gold obtainable by selling the full stock at the best known price
        long grossValue
) {}
