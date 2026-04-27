package com.tlim.character;

import com.tlim.character.dto.CharacterRequest;
import com.tlim.character.dto.CharacterResponse;
import com.tlim.server.Server;
import com.tlim.server.ServerRepository;
import com.tlim.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final ServerRepository serverRepository;
    private final UserRepository userRepository;

    public CharacterService(CharacterRepository characterRepository,
                            ServerRepository serverRepository,
                            UserRepository userRepository) {
        this.characterRepository = characterRepository;
        this.serverRepository = serverRepository;
        this.userRepository = userRepository;
    }

    public CharacterResponse createCharacter(CharacterRequest request, Long userId) {
        Server server = serverRepository.findById(request.serverId())
                .orElseThrow(() -> new EntityNotFoundException("Server not found: " + request.serverId()));

        if (characterRepository.existsByUserIdAndName(userId, request.name())) {
            throw new IllegalArgumentException(
                    "You already have a character named '" + request.name() + "'");
        }

        Character character = new Character();
        character.setName(request.name());
        character.setServer(server);
        character.setVocation(request.vocation());
        // Use a reference proxy to avoid an extra SELECT — user is already authenticated
        character.setUser(userRepository.getReferenceById(userId));

        Character saved = characterRepository.save(character);
        return toResponse(saved);
    }

    public List<CharacterResponse> getCharactersByUser(Long userId) {
        return characterRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CharacterResponse getCharacterById(Long id) {
        Character character = characterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Character not found: " + id));
        return toResponse(character);
    }

    public CharacterResponse updateCharacter(Long id, CharacterRequest request) {
        Character character = characterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Character not found: " + id));

        Server server = serverRepository.findById(request.serverId())
                .orElseThrow(() -> new EntityNotFoundException("Server not found: " + request.serverId()));

        character.setName(request.name());
        character.setServer(server);
        character.setVocation(request.vocation());

        return toResponse(characterRepository.save(character));
    }

    public void deleteCharacter(Long id) {
        if (!characterRepository.existsById(id)) {
            throw new EntityNotFoundException("Character not found: " + id);
        }
        characterRepository.deleteById(id);
    }

    private CharacterResponse toResponse(Character c) {
        return new CharacterResponse(
                c.getId(),
                c.getName(),
                c.getUser().getId(),
                c.getServer().getId(),
                c.getServer().getName(),
                c.getVocation(),
                c.getCreatedAt()
        );
    }
}
