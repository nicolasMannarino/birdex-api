package com.birdex.controller;


import com.birdex.domain.SightingRequest;
import com.birdex.service.SightingService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

