package com.birdex.service;

import com.birdex.dto.UserAchievementDto;
import com.birdex.entity.*;
import com.birdex.mapper.UserAchievementMapper;
import com.birdex.repository.AchievementRepository;
import com.birdex.repository.UserAchievementRepository;
import com.birdex.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserService userService;
    private final BirdService birdService;
    private final UserRepository userRepository;

    @Transactional
    public void checkAndUpdateAchievements(UserEntity user, BirdEntity bird, SightingEntity sighting) {
        List<AchievementEntity> allAchievements = achievementRepository.findAll();

        for (AchievementEntity achievement : allAchievements) {  
            // Buscar si ya existe el progreso del usuario en este logro
            UserAchievementEntity progress = userAchievementRepository
                    .findByUser_UserIdAndAchievement_AchievementId(user.getUserId(), achievement.getAchievementId())
                    .orElseGet(() -> UserAchievementEntity.builder()
                            .user(user)
                            .achievement(achievement)
                            .progress(new HashMap<>()) // progreso vacío
                            .build());

            if (progress.getObtainedAt() != null) continue; // ya completado

            // Evaluar y actualizar progreso
            Map<String, Object> newProgress = evaluateAndUpdateProgress(
                    achievement.getCriteria(),
                    progress.getProgress(),
                    user,
                    bird,
                    sighting
            );

            progress.setProgress(newProgress);

            // Chequear si ya cumple el logro
            if (checkCompletion(achievement.getCriteria(), newProgress)) {
                progress.setObtainedAt(LocalDateTime.now());
            }

            userAchievementRepository.save(progress);
        }
    }

    /**
     * Evalúa criterios de un logro y actualiza el progreso parcial.
     */
    private Map<String, Object> evaluateAndUpdateProgress(Map<String, Object> criteria,
                                                          Map<String, Object> currentProgress,
                                                          UserEntity user,
                                                          BirdEntity bird,
                                                          SightingEntity sighting) {
        if (currentProgress == null) currentProgress = new HashMap<>();
        Map<String, Object> updated = new HashMap<>(currentProgress);

        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (key) {
                case "sightings" -> {
                    // Primer avistamiento o N avistamientos totales
                    int total = ((Number) updated.getOrDefault("sightings", 0)).intValue() + 1;
                    updated.put("sightings", total);
                }
                case "total_sightings" -> {
                    // contar todos los avistamientos del usuario
                    int userSightings = userService.getSightingByEmail(user.getEmail()).size();
                    updated.put("total_sightings", userSightings);
                }
                case "unique_species" -> {
                    long uniqueSpecies = userService.getSightingsEntityByEmail(user.getEmail())
                            .stream()
                            .map(s -> s.getBird().getName().toLowerCase())
                            .distinct()
                            .count();
                    updated.put("unique_species", (int) uniqueSpecies);
                }
                case "rarity" -> {
                    String targetRarity = (String) value;
                    String birdRarity = bird != null ? birdService.getRarityForBird(bird) : null;
                    if (birdRarity != null && birdRarity.equalsIgnoreCase(targetRarity)) {
                        int current = ((Number) updated.getOrDefault("rarity_count", 0)).intValue() + 1;
                        updated.put("rarity_count", current);
                        updated.put("rarity", targetRarity);
                    }
                }
                case "first_of_day_before_hour" -> {
                    int hourLimit = ((Number) value).intValue();
                    if (sighting != null && sighting.getDateTime().getHour() < hourLimit) {
                        updated.put("first_of_day_before_hour", 1);
                    }
                }
            }
        }
        return updated;
    }

    /**
     * Revisa si el progreso cumple los criterios de finalización.
     */
    private boolean checkCompletion(Map<String, Object> criteria, Map<String, Object> progress) {
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            String key = entry.getKey();
            Object goal = entry.getValue();

            switch (key) {
                case "sightings", "total_sightings", "unique_species" -> {
                    int current = ((Number) progress.getOrDefault(key, 0)).intValue();
                    int required = ((Number) goal).intValue();
                    if (current < required) return false;
                }
                case "rarity" -> {
                    String requiredRarity = (String) goal;
                    String trackedRarity = (String) progress.get("rarity");
                    int count = ((Number) progress.getOrDefault("rarity_count", 0)).intValue();
                    int requiredCount = criteria.containsKey("count")
                            ? ((Number) criteria.get("count")).intValue()
                            : 1;
                    if (!requiredRarity.equalsIgnoreCase(trackedRarity) || count < requiredCount) {
                        return false;
                    }
                }
                case "first_of_day_before_hour" -> {
                    int done = ((Number) progress.getOrDefault("first_of_day_before_hour", 0)).intValue();
                    if (done < 1) return false;
                }
            }
        }
        return true;
    }

    public List<UserAchievementDto> getAchievementsByUserEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        var userAchievements = userAchievementRepository.findByUser(user);
        return UserAchievementMapper.toDtoList(userAchievements);
    }
}
