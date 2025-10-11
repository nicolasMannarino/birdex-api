package com.birdex.repository;

import com.birdex.entity.UserMissionEntity;
import com.birdex.entity.UserEntity;
import com.birdex.entity.MissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMissionRepository extends JpaRepository<UserMissionEntity, UUID> {
    List<UserMissionEntity> findByUser(UserEntity user);
    List<UserMissionEntity> findByMission(MissionEntity mission);
    Optional<UserMissionEntity> findByUser_UserIdAndMission_MissionId(UUID userId, UUID missionId);
    List<UserMissionEntity> findByUser_Email(String email);
    @Query("SELECT um FROM UserMissionEntity um " +
        "JOIN um.user u " +
        "JOIN um.mission m " +
        "WHERE u.email = :email AND m.missionId = :missionId")
    Optional<UserMissionEntity> findByUserEmailAndMissionId(@Param("email") String email,
                                                            @Param("missionId") UUID missionId);
    Optional<UserMissionEntity> findByUserMissionId(UUID userMissionId);                                                     
}
