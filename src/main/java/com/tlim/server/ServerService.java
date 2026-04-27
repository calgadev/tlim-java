package com.tlim.server;

import com.tlim.server.dto.ServerRequest;
import com.tlim.server.dto.ServerResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServerService {

    private final ServerRepository serverRepository;

    public ServerService(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }

    public ServerResponse createServer(ServerRequest request) {
        if (serverRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException("Server name already taken: " + request.name());
        }
        Server server = new Server();
        server.setName(request.name());
        server.setPvpType(request.pvpType());
        return toResponse(serverRepository.save(server));
    }

    public List<ServerResponse> getAllServers() {
        return serverRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ServerResponse getServerById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public ServerResponse updateServer(Long id, ServerRequest request) {
        Server server = findOrThrow(id);
        server.setName(request.name());
        server.setPvpType(request.pvpType());
        return toResponse(serverRepository.save(server));
    }

    public void deleteServer(Long id) {
        findOrThrow(id);
        serverRepository.deleteById(id);
    }

    private Server findOrThrow(Long id) {
        return serverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Server not found: " + id));
    }

    private ServerResponse toResponse(Server server) {
        return new ServerResponse(server.getId(), server.getName(), server.getPvpType(), server.getCreatedAt());
    }
}
