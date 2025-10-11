package com.birdex.controller;


import com.birdex.domain.AuthRequest;
import com.birdex.domain.AuthResponse;
import com.birdex.domain.RegisterRequest;
import com.birdex.entity.UserEntity;
import com.birdex.repository.UserRepository;
import com.birdex.security.JwtUtil;
import com.birdex.service.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final RegisterService registerService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        registerService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String username = auth.getName();
        String role = userRepository.findByUsername(username)
                .map(UserEntity::getRole)
                .orElse("USER");

        String token = jwtUtil.generateToken(username, role);
        return new AuthResponse(token, role);
    }
}