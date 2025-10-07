package com.birdex.repository;

import com.birdex.entity.MissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<MissionEntity, UUID> {

    /*@Query("SELECT m FROM MissionEntity m " +
           "WHERE m.user.id = :userId")
    List<MissionEntity> findActiveMissionsByUser(@Param("userId") UUID userId);*/
}
