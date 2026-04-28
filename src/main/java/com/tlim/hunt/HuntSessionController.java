package com.tlim.hunt;

import com.tlim.hunt.dto.*;
import com.tlim.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/hunt-sessions")
@Tag(name = "HuntSession")
public class HuntSessionController {

    private final HuntSessionService huntSessionService;
    private final HuntImportService huntImportService;

    public HuntSessionController(HuntSessionService huntSessionService,
                                 HuntImportService huntImportService) {
        this.huntSessionService = huntSessionService;
        this.huntImportService = huntImportService;
    }

    // Import endpoints are declared before {id} routes to prevent Spring
    // treating the literal path segment "import" as an ID value
    @Operation(summary = "Import a hunt session from Hunt Analyser text format")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Session imported"),
        @ApiResponse(responseCode = "400", description = "Missing or malformed fields in rawData"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Character not found")
    })
    @PostMapping("/import/text")
    @ResponseStatus(HttpStatus.CREATED)
    public HuntSessionResponse importFromText(@RequestBody @Valid HuntImportRequest request) {
        return huntImportService.importFromText(request);
    }

    @Operation(summary = "Import a hunt session from Hunt Analyser JSON format")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Session imported"),
        @ApiResponse(responseCode = "400", description = "Missing or malformed fields in rawData"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Character not found")
    })
    @PostMapping("/import/json")
    @ResponseStatus(HttpStatus.CREATED)
    public HuntSessionResponse importFromJson(@RequestBody @Valid HuntImportRequest request) {
        return huntImportService.importFromJson(request);
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
                                                            @RequestParam(required = false) String location,
                                                            @AuthenticationPrincipal User currentUser) {
        return huntSessionService.getSessionsByCharacter(characterId, currentUser.getId(), Optional.ofNullable(location));
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
