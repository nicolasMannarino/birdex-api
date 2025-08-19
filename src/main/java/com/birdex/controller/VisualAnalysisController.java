package com.birdex.controller;

import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import com.birdex.service.DetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/detect")
@CrossOrigin(origins = "*")
public class VisualAnalysisController {

    @Autowired
    private DetectionService detectionService;

    @PostMapping
    public ResponseEntity<BirdDetectResponse> analyze(@RequestBody BirdDetectRequest request) {
        BirdDetectResponse response = detectionService.detect(request);
        return ResponseEntity.ok(response);
    }
}
