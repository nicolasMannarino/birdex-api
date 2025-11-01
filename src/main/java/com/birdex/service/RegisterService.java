package com.birdex.service;


import com.birdex.domain.RegisterRequest;
import com.birdex.entity.*;
import com.birdex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MissionRepository missionRepository;
    private final AchievementRepository achievementRepository;
    private final UserMissionRepository userMissionRepository;
    private final UserAchievementRepository userAchievementRepository;

    @Transactional
    public void register(RegisterRequest req) {
        var user = UserEntity.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role("USER")
                .build();

        userRepository.save(user);

        var missions = missionRepository.findAll();
        for (MissionEntity m : missions) {
            var um = UserMissionEntity.builder()
                    .user(user)
                    .mission(m)
                    .progress(new HashMap<String, Object>())
                    .completed(false)
                    .claimed(false)
                    .build();
            userMissionRepository.save(um);
        }

        var achievements = achievementRepository.findAll();
        for (AchievementEntity a : achievements) {
            var ua = UserAchievementEntity.builder()
                    .user(user)
                    .achievement(a)
                    .progress(new HashMap<String, Object>())
                    .claimed(false)
                    .obtainedAt(null)
                    .build();
            userAchievementRepository.save(ua);
        }
    }
}
