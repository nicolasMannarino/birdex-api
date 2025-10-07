package com.birdex.mapper;

import com.birdex.dto.UserAchievementDto;
import com.birdex.entity.UserAchievementEntity;

import java.util.List;
import java.util.stream.Collectors;

public class UserAchievementMapper {

    public static UserAchievementDto toDto(UserAchievementEntity entity) {
        return UserAchievementDto.builder()
                .userAchievementId(entity.getUserAchievementId())
                .achievementName(entity.getAchievement().getName())
                .description(entity.getAchievement().getDescription())
                .criteria(entity.getAchievement().getCriteria())
                .progress(entity.getProgress())
                .iconUrl(entity.getAchievement().getIconUrl())
                .obtainedAt(entity.getObtainedAt())
                .build();
    }

    public static List<UserAchievementDto> toDtoList(List<UserAchievementEntity> entities) {
        return entities.stream().map(UserAchievementMapper::toDto).collect(Collectors.toList());
    }
}

