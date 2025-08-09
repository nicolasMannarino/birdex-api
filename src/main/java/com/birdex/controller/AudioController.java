package com.birdex.controller;

import com.birdex.domain.BirdnetAnalyzeRequest;
import com.birdex.domain.BirdnetAnalyzeResponse;
import com.birdex.service.BirdnetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/audio")
public class AudioController {

    private final BirdnetService birdnetService;

    public AudioController(BirdnetService birdnetService) {
        this.birdnetService = birdnetService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody Map<String, Object> body) {
        String base64 = (String) body.get("base64");
        if (base64 == null || base64.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "base64 is required"));
        }

        Double minConf = body.get("minConf") != null ? Double.parseDouble(body.get("minConf").toString()) : 0.3;

        BirdnetAnalyzeRequest req = new BirdnetAnalyzeRequest(base64, minConf);

        try {
            BirdnetAnalyzeResponse resp = birdnetService.analyze(req);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }
}
