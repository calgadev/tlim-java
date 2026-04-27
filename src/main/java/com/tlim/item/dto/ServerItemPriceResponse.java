package com.tlim.item.dto;

public record ServerItemPriceResponse(
        Long id,
        Long serverId,
        Long itemId,
        String itemName,
        Integer marketPrice
) {}
