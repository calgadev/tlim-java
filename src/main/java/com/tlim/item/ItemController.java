package com.tlim.item;

import com.tlim.item.dto.ItemRequest;
import com.tlim.item.dto.ItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/items")
@Tag(name = "Item")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @Operation(summary = "Create a new item")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Item created"),
        @ApiResponse(responseCode = "400", description = "Name is blank or already taken"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse createItem(@Valid @RequestBody ItemRequest request) {
        return itemService.createItem(request);
    }

    @Operation(summary = "List all items, optionally filtered by category or name and capped by limit")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item list returned"),
        @ApiResponse(responseCode = "400", description = "limit is not a positive number"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public List<ItemResponse> getItems(@RequestParam(required = false) String category,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) Integer limit) {
        if (limit != null && limit < 1) {
            throw new IllegalArgumentException("limit must be a positive number");
        }
        List<ItemResponse> items = (category != null && !category.isBlank())
                ? itemService.getItemsByCategory(category)
                : itemService.getAllItems(Optional.ofNullable(name));
        // Without limit the full list is returned, as before; with it, results are sorted by name first
        if (limit == null) {
            return items;
        }
        return items.stream()
                .sorted(Comparator.comparing(ItemResponse::name, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .toList();
    }

    @Operation(summary = "Get an item by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @GetMapping("/{id}")
    public ItemResponse getItemById(@PathVariable Long id) {
        return itemService.getItemById(id);
    }

    @Operation(summary = "Update an item's details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item updated"),
        @ApiResponse(responseCode = "400", description = "Name is blank"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @PutMapping("/{id}")
    public ItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return itemService.updateItem(id, request);
    }

    @Operation(summary = "Delete an item")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Item deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
    }
}
