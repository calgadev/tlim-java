package com.tlim.item;

import com.tlim.item.dto.ServerItemPriceRequest;
import com.tlim.item.dto.ServerItemPriceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/item-prices")
@Tag(name = "ServerItemPrice")
public class ServerItemPriceController {

    private final ServerItemPriceService serverItemPriceService;

    public ServerItemPriceController(ServerItemPriceService serverItemPriceService) {
        this.serverItemPriceService = serverItemPriceService;
    }

    @Operation(summary = "List all item prices for a server")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Price list returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public List<ServerItemPriceResponse> getPricesByServer(@PathVariable Long serverId) {
        return serverItemPriceService.getPricesByServer(serverId);
    }

    @Operation(summary = "Create or update a market price for a server/item pair")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Price upserted"),
        @ApiResponse(responseCode = "400", description = "marketPrice is negative or required fields are missing"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @PutMapping("/{itemId}")
    public ServerItemPriceResponse upsertMarketPrice(
            @PathVariable Long serverId,
            @PathVariable Long itemId,
            @Valid @RequestBody ServerItemPriceRequest request) {
        // Path variables are authoritative — body serverId/itemId are validated but ignored for routing
        return serverItemPriceService.upsertMarketPrice(serverId, itemId, request.marketPrice());
    }
}
