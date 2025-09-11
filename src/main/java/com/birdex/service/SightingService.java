package com.birdex.service;


import com.birdex.domain.SightingImageRequest;
import com.birdex.domain.SightingImagesByEmailResponse;
import com.birdex.exception.BirdNotFoundException;
import com.birdex.exception.UserNotFoundException;
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

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SightingService {

    private final SightingRepository sightingRepository;
    private final UserRepository userRepository;
    private final BirdRepository birdRepository;
    private final BucketService bucketService;


    public void registerSighting(SightingRequest request) {

        UserEntity userEntity = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
            log.warn("No user found for email: {}", request.getEmail());
            return new UserNotFoundException(request.getEmail());
        });

        BirdEntity birdEntity = birdRepository.findFirstByNameContainingIgnoreCase(request.getBirdName()).orElseThrow(() -> {
            log.warn("No bird found for name: {}", request.getBirdName());
            return new BirdNotFoundException(request.getBirdName());
        });

        SightingEntity sightingEntity = SightingEntity.builder()
                .dateTime(request.getDateTime())
                .location(request.getLocation())
                .user(userEntity)
                .bird(birdEntity)
                .build();

        String mimeType = FileMetadataExtractor.extractMimeType(request.getBase64());
        byte[] data = FileMetadataExtractor.extractData(request.getBase64());

        String key = FilenameGenerator.generate(request.getEmail(), request.getBirdName(), mimeType);

        String fileUrl = bucketService.upload(key, data, mimeType);
        sightingRepository.save(sightingEntity);
    }

    public SightingImagesByEmailResponse getSightingImagesByUserAndBirdName(SightingImageRequest sightingImageRequest) {
        List<String> images = bucketService.listImagesAsBase64(sightingImageRequest.getEmail(), sightingImageRequest.getBirdName());
        return SightingImagesByEmailResponse.builder()
                .base64Images(images)
                .build();
    }
}
