package com.tlim.hunt.dto;

public record HuntSessionItemResponse(
        Long id,
        Long itemId,
        String itemName,
        int quantity
) {}
