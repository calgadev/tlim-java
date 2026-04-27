package com.tlim.inventory.dto;

import java.util.List;

public record SaleDecisionResponse(
        List<ItemDecisionResponse> items,
        int passiveGold,
        int grossValue
) {}
