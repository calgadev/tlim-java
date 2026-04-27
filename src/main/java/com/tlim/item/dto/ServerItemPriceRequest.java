package com.tlim.item.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ServerItemPriceRequest(
        @NotNull Long itemId,
        @NotNull Long serverId,
        @Min(0) Integer marketPrice
) {}
