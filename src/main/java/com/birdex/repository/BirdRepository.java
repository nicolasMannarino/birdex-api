package com.birdex.repository;

import com.birdex.entity.BirdEntity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BirdRepository extends JpaRepository<BirdEntity, UUID> {
    Optional<BirdEntity> findByCommonName(String commonName);
}
