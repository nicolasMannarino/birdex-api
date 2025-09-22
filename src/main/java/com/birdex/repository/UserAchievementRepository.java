package com.birdex.repository;

import com.birdex.entity.UserAchievementEntity;
import com.birdex.entity.UserEntity;
import com.birdex.entity.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievementEntity, UUID> {
    List<UserAchievementEntity> findByUser(UserEntity user);
    List<UserAchievementEntity> findByAchievement(AchievementEntity achievement);
    boolean existsByUser_UserIdAndAchievement_AchievementId(UUID userId, UUID achievementId);
}
