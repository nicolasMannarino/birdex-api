package com.birdex.service;

import com.birdex.domain.*;
import com.birdex.dto.SightingDto;
import com.birdex.dto.SightingFullResponse;
import com.birdex.dto.SightingsForBirdResponse;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.exception.BirdNotFoundException;
import com.birdex.exception.UserNotFoundException;
import com.birdex.mapper.SightingMapper;
import com.birdex.repository.BirdRepository;
import com.birdex.repository.SightingRepository;
import com.birdex.repository.UserRepository;
import com.birdex.repository.BirdRarityRepository;
import com.birdex.utils.FileMetadataExtractor;
import com.birdex.utils.FilenameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SightingService {

    private final SightingRepository sightingRepository;
    private final UserRepository userRepository;
    private final BirdRepository birdRepository;
    private final BucketService bucketService;
    private final BirdRarityRepository birdRarityRepository;
    private final PointsService pointsService;
    private final MissionService missionService;       
    private final AchievementService achievementService; 

    public void registerSighting(SightingRequest request) {
        UserEntity userEntity = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
            log.warn("No user found for email: {}", request.getEmail());
            return new UserNotFoundException(request.getEmail());
        });

        BirdEntity birdEntity = birdRepository.findFirstByNameContainingIgnoreCase(request.getBirdName()).orElseThrow(() -> {
            log.warn("No bird found for name: {}", request.getBirdName());
            return new BirdNotFoundException(request.getBirdName());
        });

        BigDecimal lat = request.getLatitude();
        BigDecimal lon = request.getLongitude();

        if (lat == null || lon == null) {
            BigDecimal[] parsed = tryParseLatLonFromLocation(request.getLocation());
            if (parsed != null) {
                lat = parsed[0];
                lon = parsed[1];
            }
        }

        if (lat == null || lon == null) {
            throw new IllegalArgumentException("latitude/longitude son obligatorios (o pasá location=\"lat,lon\")");
        }
        validateLatLon(lat, lon);

        SightingEntity sightingEntity = SightingEntity.builder()
                .dateTime(defaultIfNull(request.getDateTime(), LocalDateTime.now()))
                .latitude(lat)
                .longitude(lon)
                .locationText(request.getLocationText())
                .user(userEntity)
                .bird(birdEntity)
                .build();

        String mimeType = FileMetadataExtractor.extractMimeType(request.getBase64());
        byte[] data = FileMetadataExtractor.extractData(request.getBase64());
        String key = FilenameGenerator.generate(request.getEmail(), request.getBirdName(), mimeType);
        String fileUrl = bucketService.upload(key, data, mimeType);

        sightingRepository.save(sightingEntity);

        int pointsAdded = pointsService.addPointsForSighting(userEntity, birdEntity);
        log.info("Se sumaron {} puntos al usuario {} por avistamiento de {}",
                pointsAdded, userEntity.getEmail(), birdEntity.getName());

        
        missionService.checkAndUpdateMissions(userEntity, birdEntity, sightingEntity);

        
        achievementService.checkAndUpdateAchievements(userEntity, birdEntity, sightingEntity);
    }

    public SightingImagesByEmailResponse getSightingImagesByUserAndBirdName(SightingImageRequest sightingImageRequest) {
        List<String> images = bucketService.listImagesAsBase64(sightingImageRequest.getEmail(), sightingImageRequest.getBirdName());
        return SightingImagesByEmailResponse.builder()
                .base64Images(images)
                .build();
    }

    public SightingByUserResponse getSightingsByUser(String email) {
        validateIfEmailExists(email);

        List<SightingEntity> entities = sightingRepository.findByUserEmail(email);
        List<SightingDto> dtos = SightingMapper.toDtoList(entities);

        List<SightingResponse> responses = buildResponseList(dtos);
        sortList(responses);

        return SightingByUserResponse.builder()
                .sightingResponseList(responses)
                .build();
    }

    public SightingsForBirdResponse getSightingsMineAndOthers(String email, String birdName) {
        userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("No user found for email: {}", email);
            return new UserNotFoundException(email);
        });

        BirdEntity bird = birdRepository.findFirstByNameContainingIgnoreCase(birdName).orElseThrow(() -> {
            log.warn("No bird found for name: {}", birdName);
            return new BirdNotFoundException(birdName);
        });

        String canonicalBirdName = bird.getName();
        String commonName = bird.getCommonName();

        String rarity = birdRarityRepository
                .findRarityNameByBirdName(canonicalBirdName)
                .orElse("");

        String profileB64 = bucketService.getBirdProfileBase64(canonicalBirdName);

        List<SightingEntity> mineEntities =
                sightingRepository.findByBird_NameIgnoreCaseAndUser_EmailOrderByDateTimeDesc(canonicalBirdName, email);

        List<SightingEntity> othersEntities =
                sightingRepository.findByBird_NameIgnoreCaseAndUser_EmailNotOrderByDateTimeDesc(canonicalBirdName, email);

        Map<String, List<String>> imagesByUserCache = new HashMap<>();

        List<SightingFullResponse> mine = mineEntities.stream()
                .map(se -> toFullResponse(se, canonicalBirdName, commonName, rarity, imagesByUserCache))
                .collect(Collectors.toList());

        List<SightingFullResponse> others = othersEntities.stream()
                .map(se -> toFullResponse(se, canonicalBirdName, commonName, rarity, imagesByUserCache))
                .collect(Collectors.toList());

        return SightingsForBirdResponse.builder()
                .birdName(canonicalBirdName)
                .commonName(commonName)
                .rarity(rarity)
                .profilePhotoBase64(profileB64)
                .mine(mine)
                .others(others)
                .build();
    }

    public Page<SightingResponse> searchSightings(String rarity, String color, String zone, String size,
                                                  int page, int sizePage) {
        Pageable pageable = PageRequest.of(page, sizePage, Sort.by(Sort.Direction.DESC, "dateTime"));

        String r = n(rarity);
        String c = n(color);
        String s = n(size);

        Page<SightingEntity> result = sightingRepository.searchSightings(r, c, s, pageable);

        List<SightingDto> dtos = SightingMapper.toDtoList(result.getContent());
        List<SightingResponse> responses = buildResponseList(dtos);
        return new PageImpl<>(responses, pageable, result.getTotalElements());
    }

    private String n(String v) {
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    private SightingFullResponse toFullResponse(SightingEntity se,
                                                String canonicalBirdName,
                                                String commonName,
                                                String rarity,
                                                Map<String, List<String>> imagesByUserCache) {

        String uEmail = se.getUser() != null ? se.getUser().getEmail() : null;
        String uName = se.getUser() != null ? se.getUser().getUsername() : null;

        List<String> imgs = imagesByUserCache.computeIfAbsent(
                uEmail != null ? uEmail : "unknown",
                k -> bucketService.listImagesAsBase64(uEmail, canonicalBirdName)
        );

        String locationString = formatLocation(se.getLatitude(), se.getLongitude(), se.getLocationText());

        return SightingFullResponse.builder()
                .sightingId(se.getSightingId())
                .birdName(canonicalBirdName)
                .commonName(commonName)
                .rarity(rarity)
                .location(locationString)
                .dateTime(se.getDateTime())
                .userEmail(uEmail)
                .username(uName)
                .imagesBase64(imgs)
                .latitude(se.getLatitude())
                .longitude(se.getLongitude())
                .build();
    }

    private void validateIfEmailExists(String email) {
        userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("No user found for email: {}", email);
            return new UserNotFoundException(email);
        });
    }

    private List<SightingResponse> buildResponseList(List<SightingDto> sightingDtos) {
        List<SightingResponse> list = new ArrayList<>();
        for (SightingDto dto : sightingDtos) {
            SightingResponse.SightingResponseBuilder builder = SightingResponse.builder()
                    .commonName(dto.birdCommonName())
                    .birdName(dto.birdName())
                    .dateTime(dto.dateTime())
                    .latitude(dto.latitude())
                    .longitude(dto.longitude());

            String rarity = birdRarityRepository.findRarityNameByBirdName(dto.birdName()).orElse("");
            builder.rarity(rarity);

            String profileBase64 = bucketService.getBirdProfileBase64(dto.birdName());
            builder.profilePhotoBase64(profileBase64);

            list.add(builder.build());
        }
        return list;
    }

    private void sortList(List<SightingResponse> sightingResponseList) {
        sightingResponseList.sort(
                Comparator.comparing(
                        SightingResponse::getDateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );
    }

    private static <T> T defaultIfNull(T v, T def) {
        return v != null ? v : def;
    }


    private static BigDecimal[] tryParseLatLonFromLocation(String location) {
        if (location == null || location.isBlank()) return null;
        String cleaned = location.trim().replaceAll("\\s+", " ");
        String[] parts = cleaned.contains(",") ? cleaned.split(",") : cleaned.split(" ");
        if (parts.length < 2) return null;
        try {
            BigDecimal lat = new BigDecimal(parts[0].trim());
            BigDecimal lon = new BigDecimal(parts[1].trim());
            return new BigDecimal[]{lat, lon};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void validateLatLon(BigDecimal lat, BigDecimal lon) {
        if (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("latitude fuera de rango (-90..90)");
        }
        if (lon.compareTo(BigDecimal.valueOf(-180)) < 0 || lon.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("longitude fuera de rango (-180..180)");
        }
    }

    private static String formatLocation(BigDecimal lat, BigDecimal lon, String locationText) {
        String latLon = (lat != null && lon != null) ? lat + "," + lon : "";
        if (locationText != null && !locationText.isBlank()) {
            return latLon.isBlank() ? locationText : (latLon + " (" + locationText + ")");
        }
        return latLon;
    }
}
