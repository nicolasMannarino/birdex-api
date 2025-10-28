package com.birdex.service;

import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import com.birdex.domain.BirdVideoDetectRequest;
import com.birdex.domain.BirdVideoDetectResponse;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.exception.UserNotFoundException;
import com.birdex.repository.SightingRepository;
import com.birdex.repository.UserRepository;
import com.birdex.utils.FileMetadataExtractor;
import com.birdex.utils.Slugs;
import com.birdex.utils.FilenameGenerator;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class DetectionService {

    private final ModelProcessor modelProcessor;
    private final SightingRepository sightingRepository;
    private final BucketService bucketService;
    private final UserRepository userRepository;
    

    private static final String CACHE = "public, max-age=31536000, immutable";

    public DetectionService(ModelProcessor modelProcessor,
                            SightingRepository sightingRepository,
                            BucketService bucketService,
                            UserRepository userRepository) {
        this.modelProcessor = modelProcessor;
        this.sightingRepository = sightingRepository;
        this.bucketService = bucketService;
        this.userRepository = userRepository;
    }

    public BirdDetectResponse detect(BirdDetectRequest request) {
        log.info("🕊️ Iniciando detección de ave para usuario: {}", request.getEmail());

        // Buscar usuario
        UserEntity userEntity = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
            log.warn("No user found for email: {}", request.getEmail());
            return new UserNotFoundException(request.getEmail());
        });

        try {
            // 1️⃣ Crear registro de avistamiento en estado PENDING
            SightingEntity pending = SightingEntity.builder()
                    .user(userEntity)
                    .dateTime(LocalDateTime.now())
                    .state("PENDING")
                    .build();

            pending = sightingRepository.save(pending);
            log.info("✅ SightingEntity creado con ID: {}", pending.getSightingId());

            // 2️⃣ Guardar archivo temporalmente en el bucket (asociado al sightingId)
            String mimeType = FileMetadataExtractor.extractMimeType(request.getFileBase64());
            byte[] data = FileMetadataExtractor.extractData(request.getFileBase64());

            String generated = FilenameGenerator.generate(
                    request.getEmail() != null ? request.getEmail() : "anonimo",
                    "pending",
                    mimeType
            );

            // 3️⃣ Ejecutar detección del modelo
            var result = modelProcessor.evaluateImage(request.getFileBase64());
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

            
            // 4️⃣ Retornar respuesta al frontend con el sightingId
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

    public BirdVideoDetectResponse detectVideo(BirdVideoDetectRequest request) {
        log.info("🎥 Iniciando detección en video (sample fps: {})",
                request.getSampleFps() != null ? request.getSampleFps() : 1);

        return modelProcessor.evaluateVideo(
                request.getFileBase64(),
                request.getSampleFps() != null ? request.getSampleFps() : 1,
                request.getStopOnFirstAbove() != null && request.getStopOnFirstAbove()
        );
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
