package com.birdex.repository;

import com.birdex.entity.BirdEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BirdRepository extends JpaRepository<BirdEntity, String> {
    Optional<BirdEntity> findByCommonName(String commonName);
}
