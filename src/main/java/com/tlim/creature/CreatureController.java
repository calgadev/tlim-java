package com.tlim.creature;

import com.tlim.creature.dto.CreatureRequest;
import com.tlim.creature.dto.CreatureResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creatures")
@Tag(name = "Creature")
public class CreatureController {

    private final CreatureService creatureService;

    public CreatureController(CreatureService creatureService) {
        this.creatureService = creatureService;
    }

    @Operation(summary = "Create a new creature")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Creature created"),
        @ApiResponse(responseCode = "400", description = "Name is blank or already taken"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatureResponse createCreature(@Valid @RequestBody CreatureRequest request) {
        return creatureService.createCreature(request);
    }

    @Operation(summary = "List all creatures")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Creature list returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public List<CreatureResponse> getAllCreatures() {
        return creatureService.getAllCreatures();
    }

    @Operation(summary = "Get a creature by ID, including its full loot list")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Creature found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Creature not found")
    })
    @GetMapping("/{id}")
    public CreatureResponse getCreatureById(@PathVariable Long id) {
        return creatureService.getCreatureById(id);
    }

    @Operation(summary = "Update a creature")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Creature updated"),
        @ApiResponse(responseCode = "400", description = "Validation error in request body"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Creature not found")
    })
    @PutMapping("/{id}")
    public CreatureResponse updateCreature(@PathVariable Long id,
                                           @Valid @RequestBody CreatureRequest request) {
        return creatureService.updateCreature(id, request);
    }

    @Operation(summary = "Delete a creature and all its loot entries")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Creature deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Creature not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCreature(@PathVariable Long id) {
        creatureService.deleteCreature(id);
    }
}
