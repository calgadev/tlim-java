package com.tlim.inventory;

import com.tlim.inventory.dto.InventoryRequest;
import com.tlim.inventory.dto.InventoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "Upsert an inventory entry for a character")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory entry created or updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping("/characters/{characterId}")
    public InventoryResponse upsertInventory(@PathVariable Long characterId,
                                             @Valid @RequestBody InventoryRequest request) {
        return inventoryService.upsertInventory(characterId, request);
    }

    @Operation(summary = "List all inventory entries for a character")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory list returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/characters/{characterId}")
    public List<InventoryResponse> getInventoryByCharacter(@PathVariable Long characterId) {
        return inventoryService.getInventoryByCharacter(characterId);
    }

    @Operation(summary = "Get a single inventory entry by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory entry found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Inventory entry not found")
    })
    @GetMapping("/{id}")
    public InventoryResponse getInventoryById(@PathVariable Long id) {
        return inventoryService.getInventoryById(id);
    }

    @Operation(summary = "Delete an inventory entry by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Inventory entry deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Inventory entry not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventoryEntry(@PathVariable Long id) {
        inventoryService.deleteInventoryEntry(id);
    }
}
