package com.birdex.controller;

import com.birdex.domain.UserPhotoRequest;
import com.birdex.dto.SightingDto;
import com.birdex.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
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

    @PatchMapping("/photo/{email}")
    public ResponseEntity<Void> updatePhoto(@PathVariable String email, @RequestBody UserPhotoRequest request) {
        userService.updatePhoto(email, request);
        return ResponseEntity.ok().build();
    }
}
