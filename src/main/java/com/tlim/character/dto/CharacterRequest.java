package com.tlim.character.dto;

import com.tlim.character.Vocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CharacterRequest(
        @NotBlank @Size(min = 2, max = 100) @Pattern(regexp = "^[a-zA-Z ]+$") String name,
        @NotNull Long serverId,
        Vocation vocation
) {}
