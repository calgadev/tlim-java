package com.tlim.inventory;

import com.tlim.character.CharacterService;
import com.tlim.character.dto.CharacterResponse;
import com.tlim.inventory.dto.InventoryRequest;
import com.tlim.inventory.dto.InventoryResponse;
import com.tlim.inventory.dto.SaleDecisionResponse;
import com.tlim.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final SaleDecisionService saleDecisionService;
    private final CharacterService characterService;

    public InventoryController(InventoryService inventoryService,
                               SaleDecisionService saleDecisionService,
                               CharacterService characterService) {
        this.inventoryService = inventoryService;
        this.saleDecisionService = saleDecisionService;
        this.characterService = characterService;
    }

    @Operation(summary = "Upsert an inventory entry for a character")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory entry created or updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Character does not belong to the authenticated user"),
        @ApiResponse(responseCode = "404", description = "Character not found")
    })
    @PostMapping("/characters/{characterId}")
    public InventoryResponse upsertInventory(@PathVariable Long characterId,
                                             @Valid @RequestBody InventoryRequest request,
                                             @AuthenticationPrincipal User currentUser) {
        return inventoryService.upsertInventory(characterId, request, currentUser.getId());
    }

    @Operation(summary = "List all inventory entries for a character")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory list returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Character does not belong to the authenticated user"),
        @ApiResponse(responseCode = "404", description = "Character not found")
    })
    @GetMapping("/characters/{characterId}")
    public List<InventoryResponse> getInventoryByCharacter(@PathVariable Long characterId,
                                                           @AuthenticationPrincipal User currentUser) {
        return inventoryService.getInventoryByCharacter(characterId, currentUser.getId());
    }

    @Operation(summary = "Compute sale decisions for all inventory items of a character")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sale decisions computed"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid serverId"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Character does not belong to the authenticated user"),
        @ApiResponse(responseCode = "404", description = "Character not found")
    })
    @GetMapping("/characters/{characterId}/decisions")
    public SaleDecisionResponse getDecisions(@PathVariable Long characterId,
                                             @RequestParam Long serverId,
                                             @AuthenticationPrincipal User currentUser) {
        CharacterResponse character = characterService.getCharacterById(characterId);
        if (!character.userId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Character does not belong to the authenticated user");
        }
        return saleDecisionService.calculateDecisions(characterId, serverId);
    }

    @Operation(summary = "Get a single inventory entry by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory entry found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Inventory entry not found")
    })
    @GetMapping("/{id}")
    public InventoryResponse getInventoryById(@PathVariable Long id,
                                              @AuthenticationPrincipal User currentUser) {
        return inventoryService.getInventoryById(id, currentUser.getId());
    }

    @Operation(summary = "Delete an inventory entry by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Inventory entry deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Inventory entry not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventoryEntry(@PathVariable Long id,
                                     @AuthenticationPrincipal User currentUser) {
        inventoryService.deleteInventoryEntry(id, currentUser.getId());
    }
}
