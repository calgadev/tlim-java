package com.tlim.hunt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HuntSessionRepository extends JpaRepository<HuntSession, Long> {

    // Most recent sessions first, matching the Stage 1 hunt history
    List<HuntSession> findByCharacterIdOrderByStartedAtDesc(Long characterId);
    List<HuntSession> findByCharacterIdAndLocationContainingIgnoreCaseOrderByStartedAtDesc(Long characterId, String location);
}
