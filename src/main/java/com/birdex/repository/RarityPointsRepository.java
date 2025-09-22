package com.birdex.repository;

import com.birdex.entity.RarityPointsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RarityPointsRepository extends JpaRepository<RarityPointsEntity, UUID> {
}
