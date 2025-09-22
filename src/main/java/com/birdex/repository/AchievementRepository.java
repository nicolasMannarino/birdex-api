package com.birdex.repository;

import com.birdex.entity.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AchievementRepository extends JpaRepository<AchievementEntity, UUID> {
}
