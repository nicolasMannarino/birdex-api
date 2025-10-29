package com.birdex.service;

import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import com.birdex.domain.BirdVideoDetectRequest;
import com.birdex.domain.BirdVideoDetectResponse;
import com.birdex.dto.enums.SIGHTING_STATUS;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.exception.UserNotFoundException;
import com.birdex.repository.SightingRepository;
import com.birdex.repository.UserRepository;
import com.birdex.utils.Base64Sanitizer;
import com.birdex.utils.FileMetadataExtractor;
import com.birdex.utils.FilenameGenerator;
import com.birdex.utils.Slugs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionService {


    private final ModelProcessor modelProcessor;
    private final SightingRepository sightingRepository;
    private final BucketService bucketService;
    private final UserRepository userRepository;
    private static final String CACHE = "public, max-age=31536000, immutable";


    public BirdVideoDetectResponse detectVideo(BirdVideoDetectRequest req) {
        int fps = (req.getSampleFps() == null || req.getSampleFps() < 1) ? 1 : req.getSampleFps();
        boolean stop = req.getStopOnFirstAbove() != null && req.getStopOnFirstAbove();
        byte[] bytes = Base64Sanitizer.decode(req.getFileBase64());
        return modelProcessor.evaluateVideo(bytes, fps, stop);
    }


    public BirdDetectResponse detect(BirdDetectRequest request) {
        log.info("🕊️ Iniciando detección de ave para usuario: {}", request.getEmail());

        UserEntity userEntity = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
            log.warn("No user found for email: {}", request.getEmail());
            return new UserNotFoundException(request.getEmail());
        });

        try {
            SightingEntity pending = SightingEntity.builder()
                    .user(userEntity)
                    .dateTime(LocalDateTime.now())
                    .state(SIGHTING_STATUS.PENDING.name())
                    .build();

            pending = sightingRepository.save(pending);
            log.info("✅ SightingEntity creado con ID: {}", pending.getSightingId());

            String mimeType = FileMetadataExtractor.extractMimeType(request.getFileBase64());
            byte[] data = FileMetadataExtractor.extractData(request.getFileBase64());

            String generated = FilenameGenerator.generate(
                    request.getEmail() != null ? request.getEmail() : "anonimo",
                    "pending",
                    mimeType
            );

            byte[] bytes = Base64Sanitizer.decode(request.getFileBase64());
            var result = modelProcessor.evaluateImage(bytes);
            log.info("🔍 Resultado del modelo: label='{}', trustLevel={}",
                    result.getLabel(), result.getTrustLevel());

            String slugBird = Slugs.of(result.getLabel());

            String keyWithinBucket = String.format("%s/%s/%s/%s",
                    request.getEmail(),
                    slugBird,
                    pending.getSightingId(),
                    lastPathSegment(generated)
            );

            /*String key = String.format(
                    "pending/%s/%s",
                    pending.getSightingId(),
                    generated
            );*/

            bucketService.uploadSightingObject(keyWithinBucket, data, toContentType(mimeType), CACHE);
            log.info("🪶 Imagen guardada en bucket con key: {}", keyWithinBucket);


            return BirdDetectResponse.builder()
                    .label(result.getLabel())
                    .trustLevel(result.getTrustLevel())
                    .sightingId(pending.getSightingId())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error durante la detección: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando detección: " + e.getMessage(), e);
        }
    }



    private static String toContentType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) return "image/jpeg";
        return mimeType;
    }

    private static String lastPathSegment(String path) {
        if (path == null || path.isBlank()) return "image";
        int i = path.lastIndexOf('/');
        return (i >= 0 && i < path.length() - 1) ? path.substring(i + 1) : path;
    }
}

