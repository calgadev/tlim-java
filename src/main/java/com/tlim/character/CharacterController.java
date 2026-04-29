package com.tlim.character;

import com.tlim.character.dto.CharacterRequest;
import com.tlim.character.dto.CharacterResponse;
import com.tlim.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@Tag(name = "Character")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @Operation(summary = "Create a new character scoped to the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Character created"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate (name, server) combination"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Server not found")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CharacterResponse createCharacter(@Valid @RequestBody CharacterRequest request,
                                             @AuthenticationPrincipal User currentUser) {
        return characterService.createCharacter(request, currentUser.getId());
    }

    @Operation(summary = "Get all characters belonging to the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of characters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public List<CharacterResponse> getCharacters(@AuthenticationPrincipal User currentUser) {
        return characterService.getCharactersByUser(currentUser.getId());
    }

    @Operation(summary = "Get a character by ID — returns 404 if the character does not belong to the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Character found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Character not found")
    })
    @GetMapping("/{id}")
    public CharacterResponse getCharacterById(@PathVariable Long id,
                                              @AuthenticationPrincipal User currentUser) {
        CharacterResponse character = characterService.getCharacterById(id);
        // Treat another user's character as non-existent to prevent data leakage
        if (!character.userId().equals(currentUser.getId())) {
            throw new EntityNotFoundException("Character not found: " + id);
        }
        return character;
    }

    @Operation(summary = "Update a character by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Character updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Character or server not found")
    })
    @PutMapping("/{id}")
    public CharacterResponse updateCharacter(@PathVariable Long id,
                                             @Valid @RequestBody CharacterRequest request,
                                             @AuthenticationPrincipal User currentUser) {
        return characterService.updateCharacter(id, request, currentUser.getId());
    }

    @Operation(summary = "Delete a character by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Character deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Character not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCharacter(@PathVariable Long id,
                                @AuthenticationPrincipal User currentUser) {
        characterService.deleteCharacter(id, currentUser.getId());
    }
}
