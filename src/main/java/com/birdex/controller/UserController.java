package com.birdex.controller;

import com.birdex.dto.SightingDto;
import com.birdex.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/sightings/{email}")
    public ResponseEntity<List<SightingDto>> getSightingsByUser(@PathVariable String email) {
        return ResponseEntity.ok(userService.getSightingByEmail(email));
    }
}
