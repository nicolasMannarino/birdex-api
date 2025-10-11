package com.birdex.controller;

import com.birdex.dto.UserMissionDto;
import com.birdex.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/missions")
@CrossOrigin(origins = "*") // permite llamadas desde cualquier frontend
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    /**
     * Obtiene todas las misiones asociadas a un usuario por su email.
     *
     * Ejemplo:
     * GET /missions/user/lucas@example.com
     */
    @GetMapping("/{email}")
    public ResponseEntity<List<UserMissionDto>> getUserMissions(@PathVariable String email) {
        List<UserMissionDto> missions = missionService.getMissionsByUserEmail(email);
        return ResponseEntity.ok(missions);
    }

    @PostMapping("/claim")
    public ResponseEntity<String> claimMissionReward(@RequestBody Map<String, String> request) {
        UUID userMissionId = UUID.fromString(request.get("userMissionId"));

        missionService.claimMissionReward(userMissionId);
        return ResponseEntity.ok("Recompensa reclamada con éxito");
    }
}
