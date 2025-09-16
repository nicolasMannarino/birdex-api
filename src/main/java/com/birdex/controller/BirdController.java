package com.birdex.controller;

import com.birdex.dto.BirdDto;
import com.birdex.dto.BirdProgressResponse;
import com.birdex.service.BirdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/bird")
@CrossOrigin(origins = "*")
public class BirdController {

    private final BirdService birdService;

    public BirdController(BirdService birdService) {
        this.birdService = birdService;
    }

    @GetMapping
    public ResponseEntity<BirdProgressResponse> getBirds() {
        BirdProgressResponse response = birdService.getBirds();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/description/{commonName}")
    public ResponseEntity<String> getDescription(@PathVariable String commonName) {
        String response = birdService.getDescription(commonName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{specificName}")
    public ResponseEntity<BirdDto> getBirdBySpecificName(@PathVariable String specificName) {
        BirdDto response = birdService.getBySpecificName(specificName);
        return ResponseEntity.ok(response);
    }
}
