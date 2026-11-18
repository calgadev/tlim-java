package com.tlim.server.dto;

import com.tlim.server.PvpType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServerRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull PvpType pvpType,
        @Size(max = 50) String location
) {}
