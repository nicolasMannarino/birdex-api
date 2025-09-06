package com.birdex.service;


import com.birdex.utils.FileMetadataExtractor;
import com.birdex.utils.FilenameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.birdex.domain.SightingRequest;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.repository.BirdRepository;
import com.birdex.repository.SightingRepository;
import com.birdex.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SightingService {

    private final SightingRepository sightingRepository;
    private final UserRepository userRepository;
    private final BirdRepository birdRepository;
    private final BucketService bucketService;


    public void registerSighting(SightingRequest request) {

        System.out.println("ASJDASLKJDASLÑKDJASKLDASLK " + request.toString());
        
        UserEntity userEntity = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
                                                                            log.warn("No user found for email: {}", request.getEmail());
                                                                            return new RuntimeException("User not found for email: " + request.getEmail());
                                                                        });
        
        BirdEntity birdEntity = birdRepository.findFirstByCommonNameContainingIgnoreCase(request.getBirdName()).orElseThrow(() -> {
                                                                            log.warn("No bird found for name: {}", request.getBirdName());
                                                                            return new RuntimeException("User not found for email: " + request.getBirdName());
                                                                        });                                                                
        
        SightingEntity sightingEntity = SightingEntity.builder()
                                .dateTime(request.getDateTime())
                                .location(request.getLocation())
                                .user(userEntity)
                                .bird(birdEntity)
                                .build();


        String mimeType = FileMetadataExtractor.extractMimeType(request.getBase64());
        byte[] data = FileMetadataExtractor.extractData(request.getBase64());
        String key = FilenameGenerator.generate(request.getEmail(), request.getBirdName());
        String fileUrl = bucketService.upload(key, data, mimeType);
    
        sightingRepository.save(sightingEntity);                                                                
    }
}
