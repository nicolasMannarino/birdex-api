package com.birdex.repository;

import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.entity.BirdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SightingRepository extends JpaRepository<SightingEntity, UUID> { }
