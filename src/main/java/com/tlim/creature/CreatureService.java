package com.tlim.creature;

import com.tlim.creature.dto.CreatureLootResponse;
import com.tlim.creature.dto.CreatureRequest;
import com.tlim.creature.dto.CreatureResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CreatureService {

    private final CreatureRepository creatureRepository;
    private final CreatureLootRepository creatureLootRepository;

    public CreatureService(CreatureRepository creatureRepository,
                           CreatureLootRepository creatureLootRepository) {
        this.creatureRepository = creatureRepository;
        this.creatureLootRepository = creatureLootRepository;
    }

    @Transactional
    public CreatureResponse createCreature(CreatureRequest request) {
        if (creatureRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException("Creature name already exists: " + request.name());
        }
        Creature creature = new Creature();
        applyRequest(creature, request);
        return toResponse(creatureRepository.save(creature), List.of());
    }

    public List<CreatureResponse> getAllCreatures() {
        return creatureRepository.findAll().stream()
                .map(c -> toResponse(c, List.of()))
                .toList();
    }

    public CreatureResponse getCreatureById(Long id) {
        Creature creature = creatureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Creature not found: " + id));
        // Resolve item names within this transaction before the lazy proxies close
        List<CreatureLootResponse> loot = creatureLootRepository.findByCreatureId(id).stream()
                .map(cl -> new CreatureLootResponse(
                        cl.getId(),
                        cl.getItem().getId(),
                        cl.getItem().getName(),
                        cl.getRarity(),
                        cl.getMinAmount(),
                        cl.getMaxAmount()))
                .toList();
        return toResponse(creature, loot);
    }

    @Transactional
    public CreatureResponse updateCreature(Long id, CreatureRequest request) {
        Creature creature = creatureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Creature not found: " + id));
        applyRequest(creature, request);
        return toResponse(creature, List.of());
    }

    @Transactional
    public void deleteCreature(Long id) {
        if (!creatureRepository.existsById(id)) {
            throw new EntityNotFoundException("Creature not found: " + id);
        }
        // Child creature_loot rows are removed by ON DELETE CASCADE (V7 migration)
        creatureRepository.deleteById(id);
    }

    private void applyRequest(Creature creature, CreatureRequest request) {
        creature.setName(request.name());
        creature.setWikiUrl(request.wikiUrl());
        creature.setImageUrl(request.imageUrl());
        creature.setHp(request.hp());
        creature.setExperience(request.experience());
        creature.setPhysicalResistance(request.physicalResistance());
        creature.setFireResistance(request.fireResistance());
        creature.setIceResistance(request.iceResistance());
        creature.setEnergyResistance(request.energyResistance());
        creature.setEarthResistance(request.earthResistance());
        creature.setDeathResistance(request.deathResistance());
        creature.setHolyResistance(request.holyResistance());
        creature.setDrownResistance(request.drownResistance());
    }

    private CreatureResponse toResponse(Creature creature, List<CreatureLootResponse> loot) {
        return new CreatureResponse(
                creature.getId(),
                creature.getName(),
                creature.getWikiUrl(),
                creature.getImageUrl(),
                creature.getHp(),
                creature.getExperience(),
                creature.getPhysicalResistance(),
                creature.getFireResistance(),
                creature.getIceResistance(),
                creature.getEnergyResistance(),
                creature.getEarthResistance(),
                creature.getDeathResistance(),
                creature.getHolyResistance(),
                creature.getDrownResistance(),
                loot,
                creature.getUpdatedAt());
    }
}
