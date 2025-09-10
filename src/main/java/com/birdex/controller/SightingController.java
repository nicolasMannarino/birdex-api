package com.birdex.controller;


import com.birdex.domain.SightingImageRequest;
import com.birdex.domain.SightingImagesByEmailResponse;
import com.birdex.domain.SightingRequest;
import com.birdex.service.SightingService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sightning")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SightingController {

    private final SightingService sightingService;

    @PostMapping
    public ResponseEntity<Void> registerSighting(@RequestBody SightingRequest request) {
        sightingService.registerSighting(request);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/{email}/{birdName}")
    public ResponseEntity<SightingImagesByEmailResponse> getSightingImagesByUserAndBirdName(@PathVariable String email,
                                                                                            @PathVariable String birdName) {
        SightingImageRequest request = SightingImageRequest.builder()
                .email(email)
                .birdName(birdName)
                .build();

        return ResponseEntity.ok(sightingService.getSightingImagesByUserAndBirdName(request));
    }
}

