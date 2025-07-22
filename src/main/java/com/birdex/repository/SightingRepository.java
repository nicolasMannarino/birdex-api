package com.birdex.repository;

import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.entity.BirdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SightingRepository extends JpaRepository<SightingEntity, String> {
    
    List<SightingEntity> findByUser(UserEntity user);

    List<SightingEntity> findByBird(BirdEntity bird);
    
    List<SightingEntity> findByUserUserID(String userID);

    List<SightingEntity> findByBirdBirdID(String birdID);
}
