package com.tlim.item.dto;

public record NpcBuyerResponse(
        Long id,
        String npcName,
        String location,
        int price
) {}
