package com.tlim.hunt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HuntImportRequest(
        @NotNull Long characterId,
        String location,
        @NotBlank String rawData,
        String name,
        Boolean isParty,
        String notes,
        Integer charLevel,
        Integer allyEkLevel,
        Integer allyMsLevel,
        Integer allyEdLevel,
        Integer allyRpLevel,
        Integer allyEmLevel
) {}
