package com.birdex.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.birdex.domain.SightingRequest;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.repository.BirdRepository;
import com.birdex.repository.SightingRepository;
import com.birdex.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Service
@Slf4j
public class SightingService {

    private final SightingRepository sightingRepository;
    private final UserRepository userRepository;
    private final BirdRepository birdRepository;

    @Autowired
    public SightingService(SightingRepository sightingRepository, UserRepository userRepository, BirdRepository birdRepository){
        this.sightingRepository = sightingRepository;
        this.userRepository = userRepository;
        this.birdRepository = birdRepository;
    }

    public void registerSighting(SightingRequest request) {
        
        UserEntity userEntity = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
                                                                            log.warn("No user found for email: {}", request.getEmail());
                                                                            return new RuntimeException("User not found for email: " + request.getEmail());
                                                                        });
        
        BirdEntity birdEntity = birdRepository.findByCommonName(request.getBirdName()).orElseThrow(() -> {
                                                                            log.warn("No user found for email: {}", request.getBirdName());
                                                                            return new RuntimeException("User not found for email: " + request.getBirdName());
                                                                        });                                                                
        
        SightingEntity sightingEntity = SightingEntity.builder()
                                .dateTime(request.getDateTime())
                                .location(request.getLocation())
                                .user(userEntity)
                                .bird(birdEntity)
                                .build();
    
        sightingRepository.save(sightingEntity);                                                                
    }
}
