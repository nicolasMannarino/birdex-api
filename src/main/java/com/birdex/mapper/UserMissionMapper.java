package com.birdex.mapper;

import com.birdex.dto.UserMissionDto;
import com.birdex.entity.UserMissionEntity;

import java.util.List;
import java.util.stream.Collectors;

public class UserMissionMapper {

    public static UserMissionDto toDto(UserMissionEntity entity) {
        if (entity == null) return null;

        var mission = entity.getMission();

        // mission.getType() en tu MissionEntity devuelve un enum/transient; para evitar problemas
        // usamos getTypeDb() (string persistido) que existe en la entidad.
        String missionType = mission != null ? mission.getTypeDb() : null;

        return UserMissionDto.builder()
                .userMissionId(entity.getUserMissionId())
                .missionName(mission != null ? mission.getName() : null)
                .missionDescription(mission != null ? mission.getDescription() : null)
                .missionType(missionType)
                .objective(mission != null ? mission.getObjective() : null)
                .progress(entity.getProgress())
                .completed(Boolean.TRUE.equals(entity.getCompleted()))
                .completedAt(entity.getCompletedAt())
                .build();
    }

    public static List<UserMissionDto> toDtoList(List<UserMissionEntity> entities) {
        return entities == null ? List.of() : entities.stream()
                .map(UserMissionMapper::toDto)
                .collect(Collectors.toList());
    }
}
