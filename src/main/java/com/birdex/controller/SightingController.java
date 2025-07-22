package com.birdex.controller;


import com.birdex.domain.SightingRequest;
import com.birdex.service.SightingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sightning")
@CrossOrigin(origins = "*")
public class SightingController {

    @Autowired
    private SightingService sightningService;

    @PostMapping
    public ResponseEntity<Void> registerSighting(@RequestBody SightingRequest request) {
        sightningService.registerSighting(request);
        return ResponseEntity.ok(null);
    }
}

