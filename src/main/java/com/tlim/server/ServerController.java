package com.tlim.server;

import com.tlim.server.dto.ServerRequest;
import com.tlim.server.dto.ServerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
@Tag(name = "Server")
public class ServerController {

    private final ServerService serverService;

    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    @Operation(summary = "Create a new server")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Server created"),
        @ApiResponse(responseCode = "400", description = "Name is blank or already taken"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServerResponse createServer(@Valid @RequestBody ServerRequest request) {
        return serverService.createServer(request);
    }

    @Operation(summary = "List all servers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Server list returned"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    public List<ServerResponse> getAllServers() {
        return serverService.getAllServers();
    }

    @Operation(summary = "Get a server by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Server found"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Server not found")
    })
    @GetMapping("/{id}")
    public ServerResponse getServerById(@PathVariable Long id) {
        return serverService.getServerById(id);
    }

    @Operation(summary = "Update a server's name and PVP type")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Server updated"),
        @ApiResponse(responseCode = "400", description = "Name is blank or pvpType is missing"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Server not found")
    })
    @PutMapping("/{id}")
    public ServerResponse updateServer(@PathVariable Long id, @Valid @RequestBody ServerRequest request) {
        return serverService.updateServer(id, request);
    }

    @Operation(summary = "Delete a server")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Server deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "404", description = "Server not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteServer(@PathVariable Long id) {
        serverService.deleteServer(id);
    }
}
