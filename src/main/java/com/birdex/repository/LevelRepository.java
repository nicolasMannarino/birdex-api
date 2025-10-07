package com.birdex.repository;

import com.birdex.entity.LevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LevelRepository extends JpaRepository<LevelEntity, Integer> {
    Optional<LevelEntity> findTopByXpRequiredLessThanEqualOrderByLevelDesc(Integer xp);
}
