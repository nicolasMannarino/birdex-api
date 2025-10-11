package com.birdex.controller;

import com.birdex.dto.UserAchievementDto;
import com.birdex.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/achievements")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    // --- Opción 1: por URL ---
    @GetMapping("/{email}")
    public ResponseEntity<List<UserAchievementDto>> getAchievementsByUser(@PathVariable String email) {
        return ResponseEntity.ok(achievementService.getAchievementsByUserEmail(email));
    }

    @PostMapping("/claim")
    public ResponseEntity<String> claimAchievement(@RequestBody Map<String, String> payload) {
        UUID userAchievementId = UUID.fromString(payload.get("userAchievementId"));
        achievementService.claimAchievement(userAchievementId);
        return ResponseEntity.ok("Logro reclamado correctamente");
    }

}

