package com.birdex.service;

import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import com.birdex.domain.BirdVideoDetectRequest;
import com.birdex.domain.BirdVideoDetectResponse;
import com.birdex.dto.enums.SightingStatus;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.exception.BirdNotFoundException;
import com.birdex.exception.UserNotFoundException;
import com.birdex.repository.BirdRepository;
import com.birdex.repository.SightingRepository;
import com.birdex.repository.UserRepository;
import com.birdex.utils.Base64Sanitizer;
import com.birdex.utils.FileMetadataExtractor;
import com.birdex.utils.FilenameGenerator;
import com.birdex.utils.Slugs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionService {

    private final ModelProcessor modelProcessor;
    private final SightingRepository sightingRepository;
    private final BucketService bucketService;
    private final UserRepository userRepository;
    private final BirdRepository birdRepository;
    private static final String CACHE = "public, max-age=31536000, immutable";

    public BirdVideoDetectResponse detectVideo(BirdVideoDetectRequest req) {
        log.info("🎥 Iniciando detección de video para usuario: {}", req.getEmail());

        int fps = (req.getSampleFps() == null || req.getSampleFps() < 1) ? 1 : req.getSampleFps();
        boolean stop = req.getStopOnFirstAbove() != null && req.getStopOnFirstAbove();
        byte[] bytes = Base64Sanitizer.decode(req.getFileBase64());

        UserEntity userEntity = userRepository.findByEmail(req.getEmail()).orElseThrow(() -> {
            log.warn("No user found for email: {}", req.getEmail());
            return new UserNotFoundException(req.getEmail());
        });

        try {
            // 1) Ejecutar el modelo
            var result = modelProcessor.evaluateVideo(bytes, fps, stop);
            log.info("🔍 Resultado del modelo de video: label='{}', trustLevel={}",
                    result.getLabel(), result.getTrustLevel());

            // 2) Guardar el archivo en el bucket
            String mimeType = FileMetadataExtractor.extractMimeType(req.getFileBase64());
            byte[] data = FileMetadataExtractor.extractData(req.getFileBase64());

            String generated = FilenameGenerator.generate(
                    req.getEmail(),
                    "pending_video",
                    mimeType
            );

            String slugBird = Slugs.of(result.getLabel());

            BirdEntity bird = birdRepository.findFirstByNameContainingIgnoreCase(slugBird.replace("_", " "))
                    .orElseThrow(() -> {
                        log.warn("No bird found for name: {}", slugBird);
                        return new BirdNotFoundException(slugBird);
                    });

            SightingEntity pending = SightingEntity.builder()
                    .user(userEntity)
                    .bird(bird)
                    .dateTime(LocalDateTime.now())
                    .state(SightingStatus.PENDING.name())
                    .build();

            pending = sightingRepository.save(pending);
            log.info("✅ SightingEntity creado con ID: {}", pending.getSightingId());

            String keyWithinBucket = String.format("%s/%s/%s/%s",
                    req.getEmail(),
                    slugBird,
                    pending.getSightingId(),
                    lastPathSegment(generated)
            );

            // 👇 acá estaba el error: ahora usamos el fallback correcto para VIDEO
            bucketService.uploadSightingObject(
                    keyWithinBucket,
                    data,
                    toContentType(mimeType, "video/mp4"),
                    CACHE
            );
            log.info("🎞️ Video guardado en bucket con key: {}", keyWithinBucket);

            return BirdVideoDetectResponse.builder()
                    .label(result.getLabel())
                    .trustLevel(result.getTrustLevel())
                    .sightingId(pending.getSightingId())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error durante la detección de video: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando detección de video: " + e.getMessage(), e);
        }
    }


    public BirdDetectResponse detect(BirdDetectRequest request) {
        log.info("🕊️ Iniciando detección de ave para usuario: {}", request.getEmail());

        UserEntity userEntity = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> {
            log.warn("No user found for email: {}", request.getEmail());
            return new UserNotFoundException(request.getEmail());
        });

        try {
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

            BirdEntity bird = birdRepository.findFirstByNameContainingIgnoreCase(slugBird.replace("_", " "))
                    .orElseThrow(() -> {
                        log.warn("No bird found for name: {}", slugBird);
                        return new BirdNotFoundException(slugBird);
                    });

            SightingEntity pending = SightingEntity.builder()
                    .user(userEntity)
                    .bird(bird)
                    .dateTime(LocalDateTime.now())
                    .state(SightingStatus.PENDING.name())
                    .build();

            pending = sightingRepository.save(pending);
            log.info("✅ SightingEntity creado con ID: {}", pending.getSightingId());

            String keyWithinBucket = String.format("%s/%s/%s/%s",
                    request.getEmail(),
                    slugBird,
                    pending.getSightingId(),
                    lastPathSegment(generated)
            );

            // 👇 acá también estaba llamando al método con 1 solo parámetro
            bucketService.uploadSightingObject(
                    keyWithinBucket,
                    data,
                    toContentType(mimeType, "image/jpeg"),
                    CACHE
            );
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

    public BirdVideoDetectResponse detectVideoMultipart(MultipartFile file,
                                                        String email,
                                                        Integer sampleFps,
                                                        Boolean stopOnFirstAbove) {
        log.info("🎥 (multipart) Iniciando detección de video para usuario: {}", email);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de video es obligatorio");
        }

        UserEntity userEntity = userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("No user found for email: {}", email);
            return new UserNotFoundException(email);
        });

        try {
            byte[] data = file.getBytes();
            int fps = (sampleFps == null || sampleFps < 1) ? 1 : sampleFps;
            boolean stop = stopOnFirstAbove != null && stopOnFirstAbove;

            var result = modelProcessor.evaluateVideoMultipart(data, fps, stop);
            log.info("🔍 (multipart) Resultado del modelo de video: label='{}', trustLevel={}",
                    result.getLabel(), result.getTrustLevel());

            String slugBird = Slugs.of(result.getLabel());

            BirdEntity bird = birdRepository.findFirstByNameContainingIgnoreCase(slugBird.replace("_", " "))
                    .orElseThrow(() -> {
                        log.warn("No bird found for name: {}", slugBird);
                        return new BirdNotFoundException(slugBird);
                    });

            SightingEntity pending = SightingEntity.builder()
                    .user(userEntity)
                    .bird(bird)
                    .dateTime(LocalDateTime.now())
                    .state(SightingStatus.PENDING.name())
                    .build();

            pending = sightingRepository.save(pending);
            log.info("✅ (multipart) SightingEntity creado con ID: {}", pending.getSightingId());

            String originalName = file.getOriginalFilename();
            String mimeType = file.getContentType();
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "video/mp4";
            }

            String keyWithinBucket = String.format("%s/%s/%s/%s",
                    email,
                    slugBird,
                    pending.getSightingId(),
                    lastPathSegment(
                            originalName != null
                                    ? originalName
                                    : FilenameGenerator.generate(email, "pending_video", mimeType)
                    )
            );

            bucketService.uploadSightingObject(keyWithinBucket, data, mimeType, CACHE);
            log.info("🎞️ (multipart) Video guardado en bucket con key: {}", keyWithinBucket);

            return BirdVideoDetectResponse.builder()
                    .label(result.getLabel())
                    .trustLevel(result.getTrustLevel())
                    .sightingId(pending.getSightingId())
                    .build();

        } catch (IOException e) {
            log.error("❌ Error leyendo el archivo multipart: {}", e.getMessage(), e);
            throw new RuntimeException("Error leyendo el archivo multipart: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error durante la detección multipart: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando detección multipart: " + e.getMessage(), e);
        }
    }

    /* ===== utils ===== */

    private static String toContentType(String mimeType, String fallback) {
        return (mimeType == null || mimeType.isBlank()) ? fallback : mimeType;
    }

    private static String lastPathSegment(String path) {
        if (path == null || path.isBlank()) return "file";
        int i = path.lastIndexOf('/');
        return (i >= 0 && i < path.length() - 1) ? path.substring(i + 1) : path;
    }
}
