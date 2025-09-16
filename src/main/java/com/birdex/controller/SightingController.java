package com.birdex.controller;


import com.birdex.domain.*;
import com.birdex.dto.SightingsForBirdResponse;
import com.birdex.service.SightingService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sighting")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SightingController {

    private final SightingService sightingService;

    @PostMapping
    public ResponseEntity<Void> registerSighting(@RequestBody SightingRequest request) {
        sightingService.registerSighting(request);
        return ResponseEntity.noContent().build();
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

    @GetMapping("/{email}/{birdName}/all")
    public ResponseEntity<SightingsForBirdResponse> getSightingsMineAndOthers(@PathVariable String email,
                                                                              @PathVariable String birdName) {
        return ResponseEntity.ok(sightingService.getSightingsMineAndOthers(email, birdName));
    }

    @GetMapping("/{email}")
    public ResponseEntity<SightingByUserResponse> getSightingsByUser(@PathVariable String email) {
        return ResponseEntity.ok(sightingService.getSightingsByUser(email));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SightingResponse>> searchSightings(
            @RequestParam(required = false) String rarity,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String size,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int sizePage
    ) {
        return ResponseEntity.ok(sightingService.searchSightings(rarity, color, zone, size, page, sizePage));
    }

}

