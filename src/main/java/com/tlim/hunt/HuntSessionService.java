package com.tlim.hunt;

import com.tlim.character.CharacterRepository;
import com.tlim.hunt.dto.*;
import com.tlim.inventory.InventoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class HuntSessionService {

    private final HuntSessionRepository huntSessionRepository;
    private final HuntSessionItemRepository huntSessionItemRepository;
    private final HuntSessionMonsterRepository huntSessionMonsterRepository;
    private final CharacterRepository characterRepository;
    private final InventoryRepository inventoryRepository;

    public HuntSessionService(HuntSessionRepository huntSessionRepository,
                              HuntSessionItemRepository huntSessionItemRepository,
                              HuntSessionMonsterRepository huntSessionMonsterRepository,
                              CharacterRepository characterRepository,
                              InventoryRepository inventoryRepository) {
        this.huntSessionRepository = huntSessionRepository;
        this.huntSessionItemRepository = huntSessionItemRepository;
        this.huntSessionMonsterRepository = huntSessionMonsterRepository;
        this.characterRepository = characterRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public List<HuntSessionResponse> getSessionsByCharacter(Long characterId, Long currentUserId, Optional<String> location) {
        var character = characterRepository.findById(characterId)
                .orElseThrow(() -> new EntityNotFoundException("Character not found: " + characterId));
        // Ownership check always runs before any repository call — location filter cannot bypass it
        if (!character.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }
        List<HuntSession> sessions = (location.isPresent() && !location.get().isBlank())
                ? huntSessionRepository.findByCharacterIdAndLocationContainingIgnoreCaseOrderByStartedAtDesc(
                        characterId, location.get().trim())
                : huntSessionRepository.findByCharacterIdOrderByStartedAtDesc(characterId);
        return sessions.stream().map(this::toResponse).toList();
    }

    public HuntSessionResponse getSessionById(Long id, Long currentUserId) {
        HuntSession session = huntSessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hunt session not found: " + id));
        // Treat another user's session as non-existent to prevent data leakage
        if (!session.getCharacter().getUser().getId().equals(currentUserId)) {
            throw new EntityNotFoundException("Hunt session not found: " + id);
        }
        return toResponse(session);
    }

    @Transactional
    public void deleteSession(Long id, Long currentUserId) {
        HuntSession session = huntSessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hunt session not found: " + id));
        if (!session.getCharacter().getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        // Undo what the import added to the inventory; stock never goes below zero
        // (the user may already have sold or adjusted part of it).
        Long characterId = session.getCharacter().getId();
        List<HuntSessionItem> sessionItems = huntSessionItemRepository.findByHuntSessionId(session.getId());
        for (HuntSessionItem sessionItem : sessionItems) {
            inventoryRepository.findByCharacterIdAndItemId(characterId, sessionItem.getItem().getId())
                    .ifPresent(inventory -> {
                        inventory.setCurrentQuantity(
                                Math.max(0, inventory.getCurrentQuantity() - sessionItem.getQuantity()));
                        inventoryRepository.save(inventory);
                    });
        }

        // Children are removed explicitly: they are loaded in this persistence context, so relying on
        // the database ON DELETE CASCADE alone would leave Hibernate holding references to a deleted session
        huntSessionItemRepository.deleteAll(sessionItems);
        huntSessionMonsterRepository.deleteAll(huntSessionMonsterRepository.findByHuntSessionId(session.getId()));
        huntSessionRepository.delete(session);
    }

    HuntSessionResponse toResponse(HuntSession session) {
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

        return buildResponse(session, items, monsters, List.of(), List.of());
    }

    static HuntSessionResponse buildResponse(HuntSession session,
                                             List<HuntSessionItemResponse> items,
                                             List<HuntSessionMonsterResponse> monsters,
                                             List<String> skippedItems,
                                             List<String> skippedMonsters) {
        return new HuntSessionResponse(
                session.getId(),
                session.getCharacter().getId(),
                session.getName(),
                session.getLocation(),
                session.isParty(),
                session.getNotes(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDuration(),
                session.getRawXp(),
                session.getXpWithBonus(),
                session.getLootTotal(),
                session.getSupplies(),
                // balance is never stored — both inputs are immutable after import
                session.getLootTotal() - session.getSupplies(),
                session.getDamage(),
                session.getHealing(),
                session.getCharLevel(),
                session.getAllyEkLevel(),
                session.getAllyMsLevel(),
                session.getAllyEdLevel(),
                session.getAllyRpLevel(),
                session.getAllyEmLevel(),
                items,
                monsters,
                skippedItems,
                skippedMonsters
        );
    }
}
