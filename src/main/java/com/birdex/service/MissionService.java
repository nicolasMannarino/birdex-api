package com.birdex.service;

import com.birdex.entity.BirdEntity;
import com.birdex.entity.MissionEntity;
import com.birdex.entity.UserMissionEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.repository.UserMissionRepository;
import com.birdex.repository.BirdRepository;
import com.birdex.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import com.birdex.dto.UserMissionDto;
import com.birdex.mapper.UserMissionMapper;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final BirdService birdService;

    @Transactional
    public void checkAndUpdateMissions(UserEntity user, BirdEntity bird, SightingEntity sighting) {
        // 1. Traer misiones activas del usuario
        List<UserMissionEntity> activeMissions = userMissionRepository.findByUser(user);

        for (UserMissionEntity userMissionEntity : activeMissions) {
            MissionEntity missionEntity = userMissionEntity.getMission();
            // 2. Traer progreso actual
            /**UserMissionEntity userMission = userMissionRepository
                    .findByUser_UserIdAndMission_MissionId(user.getUserId(), mission.getMissionId())
                    .orElseGet(() -> {
                        UserMissionEntity newProgress = new UserMissionEntity();
                        newProgress.setUser(user);
                        newProgress.setMission(mission);
                        newProgress.setProgress(new java.util.HashMap<>()); // empieza vacío
                        newProgress.setCompleted(false);
                        return newProgress;
                    });*/

            if (Boolean.TRUE.equals(userMissionEntity.getCompleted())) {
                continue; // ya completada, no hacemos nada
            }

            Map<String, Object> objective = missionEntity.getObjective();
            Map<String, Object> progress = userMissionEntity.getProgress();
            if (progress == null) progress = new java.util.HashMap<>();

            boolean updated = false;

            // 3. Evaluar condiciones
            if (objective.containsKey("sightings")) {
                // Simple: contar avistamientos
                int current = ((Number) progress.getOrDefault("sightings", 0)).intValue();
                progress.put("sightings", current + 1);
                updated = true;
            }

            if (objective.containsKey("rarity")) {
                String targetRarity = (String) objective.get("rarity");
                String birdRarity = birdService.getRarityForBird(bird);

                    if (targetRarity != null && targetRarity.equalsIgnoreCase(birdRarity)) {
                        int current = ((Number) progress.getOrDefault("count", 0)).intValue();
                        progress.put("count", current + 1);
                        updated = true;
                    }
                }

            if (objective.containsKey("province")) {
                String targetProvince = (String) objective.get("province");
                String sightingProvince = sighting.getLocationText();

                if (targetProvince != null && targetProvince.equalsIgnoreCase(sightingProvince)) {
                    int current = ((Number) progress.getOrDefault("count", 0)).intValue();
                    progress.put("count", current + 1);
                    updated = true;
                }
            }

            // 4. Verificar si se completó
            boolean completed = checkCompletion(objective, progress);
            if (completed) {
                userMissionEntity.setCompleted(true);
                userMissionEntity.setCompletedAt(LocalDateTime.now());
            }

            if (updated) {
                userMissionEntity.setProgress(progress);
                userMissionRepository.save(userMissionEntity);
            }
            }
    }

    private boolean checkCompletion(Map<String, Object> objective, Map<String, Object> progress) {
        for (Map.Entry<String, Object> entry : objective.entrySet()) {
            String key = entry.getKey();
            Object required = entry.getValue();

            if (required instanceof Number requiredNum) {
                int current = ((Number) progress.getOrDefault(key, 0)).intValue();
                if (current < requiredNum.intValue()) {
                    return false;
                }
            } else if (required instanceof String requiredStr) {
                Object current = progress.get(key);
                if (current == null || !requiredStr.equalsIgnoreCase(current.toString())) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<UserMissionDto> getMissionsByUserEmail(String email) {
        var userMissions = userMissionRepository.findByUser_Email(email);
        return UserMissionMapper.toDtoList(userMissions);
    }
}

