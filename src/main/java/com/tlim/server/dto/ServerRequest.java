package com.tlim.server.dto;

import com.tlim.server.PvpType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServerRequest(
        @NotBlank String name,
        @NotNull PvpType pvpType
) {}
