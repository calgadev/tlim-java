package com.tlim.item.dto;

import jakarta.validation.constraints.Min;

// serverId and itemId come from the URL path. A null marketPrice clears the registered price.
public record ServerItemPriceRequest(
        @Min(0) Integer marketPrice
) {}
