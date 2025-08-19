package com.birdex.controller;

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

    @GetMapping("/description/{commonName}")
    public ResponseEntity<String> getDescription(@PathVariable String commonName) {
        String response = birdService.getDescription(commonName);
        return ResponseEntity.ok(response);
    }
}
