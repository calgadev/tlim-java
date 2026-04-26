package com.tlim.hunt;

import com.tlim.hunt.dto.*;
import com.tlim.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hunt-sessions")
@Tag(name = "HuntSession")
public class HuntSessionController {

    private final HuntSessionService huntSessionService;

    public HuntSessionController(HuntSessionService huntSessionService) {
        this.huntSessionService = huntSessionService;
    }

    @Operation(summary = "List all hunt sessions for a character")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessions returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Character does not belong to the authenticated user"),
        @ApiResponse(responseCode = "404", description = "Character not found")
    })
    @GetMapping("/characters/{characterId}")
    public List<HuntSessionResponse> getSessionsByCharacter(@PathVariable Long characterId,
                                                            @AuthenticationPrincipal User currentUser) {
        return huntSessionService.getSessionsByCharacter(characterId, currentUser.getId());
    }

    @Operation(summary = "Get a hunt session by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @GetMapping("/{id}")
    public HuntSessionResponse getSessionById(@PathVariable Long id) {
        return huntSessionService.getSessionById(id);
    }

    @Operation(summary = "Delete a hunt session by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Session does not belong to the authenticated user"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable Long id,
                              @AuthenticationPrincipal User currentUser) {
        huntSessionService.deleteSession(id, currentUser.getId());
    }
}
