package com.birdex.service;

import com.birdex.entity.BirdEntity;
import com.birdex.entity.LevelEntity;
import com.birdex.entity.UserEntity;
import com.birdex.repository.BirdRarityRepository;
import com.birdex.repository.LevelRepository;
import com.birdex.repository.RarityPointsRepository;
import com.birdex.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointsService {

    private final BirdRarityRepository birdRarityRepository;
    private final RarityPointsRepository rarityPointsRepository;
    private final UserRepository userRepository;
    private final LevelRepository levelRepository;

    public PointsService(
            BirdRarityRepository birdRarityRepository,
            RarityPointsRepository rarityPointsRepository,
            UserRepository userRepository,
            LevelRepository levelRepository
    ) {
        this.birdRarityRepository = birdRarityRepository;
        this.rarityPointsRepository = rarityPointsRepository;
        this.userRepository = userRepository;
        this.levelRepository = levelRepository;
    }

    /**
     * Suma puntos al usuario en base a la rareza del ave avistada.
     * Además controla si sube de nivel.
     * @param user Usuario al que se suman los puntos
     * @param bird Ave avistada
     * @return Puntos sumados
     */
    @Transactional
    public int addPointsForSighting(UserEntity user, BirdEntity bird) {
        // 1. Obtener rareza del ave
        String rarity = birdRarityRepository
                .findRarityNameByBirdName(bird.getName())
                .orElse("Común");

        // 2. Puntos por rareza
        int points = rarityPointsRepository.findPointsByRarity(rarity).orElse(0);

        // 3. Sumar al total del usuario
        int newTotal = (user.getPoints() == null ? 0 : user.getPoints()) + points;
        user.setPoints(newTotal);

        // 4. Verificar nivel más alto alcanzable
        levelRepository.findTopByXpRequiredLessThanEqualOrderByLevelDesc(newTotal)
                .ifPresent(level -> {
                    if (level.getLevel() > user.getLevel()) {
                        user.setLevel(level.getLevel());
                        user.setLevelName(level.getName());
                    }
                });

        // 5. Guardar cambios
        userRepository.save(user);

        return points;
    }
}
