package com.birdex.service;


import com.birdex.domain.*;
import com.birdex.dto.SightingDto;
import com.birdex.exception.BirdNotFoundException;
import com.birdex.exception.UserNotFoundException;
import com.birdex.mapper.SightingMapper;
import com.birdex.repository.BirdRarityRepository;
import com.birdex.utils.FileMetadataExtractor;
import com.birdex.utils.FilenameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.birdex.entity.BirdEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.repository.BirdRepository;
import com.birdex.repository.SightingRepository;
import com.birdex.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SightingService {

    private final SightingRepository sightingRepository;
    private final UserRepository userRepository;
    private final BirdRepository birdRepository;
    private final BucketService bucketService;
    private final BirdRarityRepository birdRarityRepository;


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

    public SightingByUserResponse getSightingsByUser(String email) {
        validateIfEmailExists(email);

        List<SightingEntity> sightingEntity = sightingRepository.findByUserEmail(email);
        List<SightingDto> sightingDtos = SightingMapper.toDtoList(sightingEntity);

        List<SightingResponse> sightingResponseList = buildResponseList(sightingDtos);
        sortList(sightingResponseList);

        return SightingByUserResponse.builder()
                .sightingResponseList(sightingResponseList)
                .build();

    }

    private void validateIfEmailExists(String email) {
        userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("No user found for email: {}", email);
            return new UserNotFoundException(email);
        });
    }

    private List<SightingResponse> buildResponseList(List<SightingDto> sightingDtos) {
        List<SightingResponse> sightingResponseList = new ArrayList<>();
        for (SightingDto dto : sightingDtos) {

            SightingResponse.SightingResponseBuilder builder = SightingResponse.builder();
            builder.commonName(dto.birdCommonName())
                    .birdName(dto.birdName())
                    .dateTime(dto.dateTime());

            Optional<String> rarity = birdRarityRepository.findRarityNameByBirdName(dto.birdName());
            builder.rarity(rarity.get());
            String profileBase64 = bucketService.getBirdProfileBase64(dto.birdName());
            builder.profilePhotoBase64(profileBase64);

            SightingResponse response = builder.build();

            sightingResponseList.add(response);
        }

        return sightingResponseList;
    }


    private void sortList(List<SightingResponse> sightingResponseList) {
        sightingResponseList.sort(
                Comparator.comparing(
                        SightingResponse::getDateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );
    }


}
