package com.birdex.controller;

import com.birdex.domain.BirdnetAnalyzeRequest;
import com.birdex.domain.Detection;
import com.birdex.service.BirdnetService;
import jakarta.validation.Valid;
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
    public ResponseEntity<Detection> analyze(@RequestBody @Valid BirdnetAnalyzeRequest request) {
        Detection resp = birdnetService.analyze(request);
        return ResponseEntity.ok().body(resp);
    }
}
