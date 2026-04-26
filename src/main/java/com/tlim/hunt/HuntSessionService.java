package com.tlim.hunt;

import com.tlim.character.CharacterRepository;
import com.tlim.hunt.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HuntSessionService {

    private final HuntSessionRepository huntSessionRepository;
    private final HuntSessionItemRepository huntSessionItemRepository;
    private final HuntSessionMonsterRepository huntSessionMonsterRepository;
    private final CharacterRepository characterRepository;

    public HuntSessionService(HuntSessionRepository huntSessionRepository,
                              HuntSessionItemRepository huntSessionItemRepository,
                              HuntSessionMonsterRepository huntSessionMonsterRepository,
                              CharacterRepository characterRepository) {
        this.huntSessionRepository = huntSessionRepository;
        this.huntSessionItemRepository = huntSessionItemRepository;
        this.huntSessionMonsterRepository = huntSessionMonsterRepository;
        this.characterRepository = characterRepository;
    }

    public List<HuntSessionResponse> getSessionsByCharacter(Long characterId, Long currentUserId) {
        var character = characterRepository.findById(characterId)
                .orElseThrow(() -> new EntityNotFoundException("Character not found: " + characterId));
        if (!character.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }
        return huntSessionRepository.findByCharacterId(characterId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public HuntSessionResponse getSessionById(Long id) {
        return huntSessionRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Hunt session not found: " + id));
    }

    @Transactional
    public void deleteSession(Long id, Long currentUserId) {
        HuntSession session = huntSessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hunt session not found: " + id));
        if (!session.getCharacter().getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }
        huntSessionRepository.delete(session);
    }

    private HuntSessionResponse toResponse(HuntSession session) {
        List<HuntSessionItemResponse> items = huntSessionItemRepository
                .findByHuntSessionId(session.getId())
                .stream()
                .map(i -> new HuntSessionItemResponse(
                        i.getId(),
                        i.getItem().getId(),
                        i.getItem().getName(),
                        i.getQuantity()))
                .toList();

        List<HuntSessionMonsterResponse> monsters = huntSessionMonsterRepository
                .findByHuntSessionId(session.getId())
                .stream()
                .map(m -> new HuntSessionMonsterResponse(
                        m.getId(),
                        m.getCreature().getId(),
                        m.getCreature().getName(),
                        m.getKillCount()))
                .toList();

        return new HuntSessionResponse(
                session.getId(),
                session.getCharacter().getId(),
                session.getLocation(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDuration(),
                session.getRawXp(),
                session.getXpWithBonus(),
                session.getLootTotal(),
                session.getSupplies(),
                session.getDamage(),
                session.getHealing(),
                items,
                monsters
        );
    }
}
