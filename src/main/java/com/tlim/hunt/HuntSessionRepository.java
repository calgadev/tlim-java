package com.tlim.hunt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HuntSessionRepository extends JpaRepository<HuntSession, Long> {

    List<HuntSession> findByCharacterId(Long characterId);
    List<HuntSession> findByCharacterIdAndLocationContainingIgnoreCase(Long characterId, String location);
}
