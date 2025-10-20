package com.birdex.service;

import com.birdex.entity.BirdEntity;
import com.birdex.entity.MissionEntity;
import com.birdex.entity.UserMissionEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.repository.UserMissionRepository;
import com.birdex.repository.UserRepository;
import com.birdex.repository.BirdRepository;
import com.birdex.repository.LevelRepository;
import com.birdex.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import com.birdex.dto.UserMissionDto;
import com.birdex.mapper.UserMissionMapper;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final BirdService birdService;
    private final UserRepository userRepository;
    private final PointsService pointsService;
    private final LevelRepository levelRepository;

    @Transactional
    public void checkAndUpdateMissions(UserEntity user, BirdEntity bird, SightingEntity sighting) {
        // 1. Traer misiones activas del usuario
        List<UserMissionEntity> activeMissions = userMissionRepository.findByUser(user);

        for (UserMissionEntity userMissionEntity : activeMissions) {
            MissionEntity missionEntity = userMissionEntity.getMission();

            if (Boolean.TRUE.equals(userMissionEntity.getCompleted())) {
                continue; // ya completada, no hacemos nada
            }

            Map<String, Object> objective = missionEntity.getObjective();
            Map<String, Object> progress = userMissionEntity.getProgress();
            if (progress == null) progress = new HashMap<>();

            boolean updated = false;

            // 2. Evaluar condiciones según el tipo de objetivo
            for (Map.Entry<String, Object> entry : objective.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                switch (key) {
                    case "sightings" -> {
                        int current = ((Number) progress.getOrDefault("sightings", 0)).intValue();
                        progress.put("sightings", current + 1);
                        updated = true;
                    }

                    case "rarity" -> {
                        String targetRarity = (String) value;
                        String birdRarity = birdService.getRarityForBird(bird);

                        if (birdRarity != null && birdRarity.equalsIgnoreCase(targetRarity)) {
                            int current = ((Number) progress.getOrDefault("count", 0)).intValue() + 1;
                            progress.put("count", current);
                            progress.put("rarity", targetRarity); // ✅ importante para validación
                            updated = true;
                        }
                    }

                    case "province" -> {
                        String targetProvince = (String) value;
                        String sightingProvince = sighting.getLocationText();

                        if (targetProvince != null && targetProvince.equalsIgnoreCase(sightingProvince)) {
                            int current = ((Number) progress.getOrDefault("count", 0)).intValue() + 1;
                            progress.put("count", current);
                            updated = true;
                        }
                    }

                    
                }
            }

            // 3. Verificar si se completó
            boolean completed = checkCompletion(objective, progress);
            if (completed) {
                userMissionEntity.setCompleted(true);
                userMissionEntity.setCompletedAt(LocalDateTime.now());
            }

            // 4. Guardar si hubo cambios
            if (updated || completed) {
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

    @Transactional
    public void claimMissionReward(UUID userMissionId) {
        var userMissionOpt = userMissionRepository.findByUserMissionId(userMissionId);

        if (userMissionOpt.isEmpty()) {
            throw new RuntimeException("No se encontró la misión para el usuario especificado");
        }

        UserMissionEntity userMission = userMissionOpt.get();

        if (!Boolean.TRUE.equals(userMission.getCompleted())) {
            throw new RuntimeException("La misión aún no está completada");
        }

        if (Boolean.TRUE.equals(userMission.getClaimed())) {
            throw new RuntimeException("La recompensa de esta misión ya fue reclamada");
        }

        // Marcar como reclamada
        userMission.setClaimed(true);
        userMissionRepository.save(userMission);

        // Obtener usuario y misión
        var user = userMission.getUser();
        var mission = userMission.getMission();

        // Sumar puntos de recompensa
        int currentPoints = (user.getPoints() == null ? 0 : user.getPoints());
        int newTotal = currentPoints + mission.getRewardPoints();
        user.setPoints(newTotal);

        // Verificar nivel alcanzable desde la tabla 'levels'
        pointsServiceCheckLevelUp(user, newTotal);

        userRepository.save(user);
    }

    /**
     * Controla el nivel del usuario en base a su XP total.
     */
    private void pointsServiceCheckLevelUp(com.birdex.entity.UserEntity user, int newTotal) {
        //var levelRepo = pointsService.getLevelRepository();
        levelRepository.findTopByXpRequiredLessThanEqualOrderByLevelDesc(newTotal)
                .ifPresent(level -> {
                    if (level.getLevel() > user.getLevel()) {
                        user.setLevel(level.getLevel());
                        user.setLevelName(level.getName());
                    }
                });
    }

}

