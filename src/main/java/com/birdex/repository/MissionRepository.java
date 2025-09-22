package com.birdex.repository;

import com.birdex.entity.MissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MissionRepository extends JpaRepository<MissionEntity, UUID> {
}
