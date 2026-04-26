package com.tlim.hunt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HuntSessionItemRepository extends JpaRepository<HuntSessionItem, Long> {

    List<HuntSessionItem> findByHuntSessionId(Long huntSessionId);
}
