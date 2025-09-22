package com.birdex.repository;

import com.birdex.entity.UserMissionEntity;
import com.birdex.entity.UserEntity;
import com.birdex.entity.MissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMissionRepository extends JpaRepository<UserMissionEntity, UUID> {
    List<UserMissionEntity> findByUser(UserEntity user);
    List<UserMissionEntity> findByMission(MissionEntity mission);
    Optional<UserMissionEntity> findByUser_UserIdAndMission_MissionId(UUID userId, UUID missionId);
}
