package com.tlim.hunt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HuntSessionMonsterRepository extends JpaRepository<HuntSessionMonster, Long> {

    List<HuntSessionMonster> findByHuntSessionId(Long huntSessionId);
}
