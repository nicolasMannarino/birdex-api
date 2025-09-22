package com.birdex.repository;

import com.birdex.entity.LevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelRepository extends JpaRepository<LevelEntity, Integer> {
    LevelEntity findTopByXpRequiredLessThanEqualOrderByLevelDesc(Integer xp);
}
