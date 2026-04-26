package com.tlim.character.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CharacterRequest(
        @NotBlank String name,
        @NotNull Long serverId
) {}
