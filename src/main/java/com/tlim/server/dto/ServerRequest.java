package com.tlim.server.dto;

import jakarta.validation.constraints.NotBlank;

public record ServerRequest(
        @NotBlank String name
) {}
