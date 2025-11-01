package com.birdex.service;

import com.birdex.client.AudioAIModelClient;
import com.birdex.domain.BirdnetAnalyzeRequest;
import com.birdex.domain.BirdnetAnalyzeResponse;
import com.birdex.domain.Detection;
import com.birdex.dto.enums.SightingStatus;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

import static com.birdex.utils.DefaultResponse.defaultDetectionResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class BirdnetService {

    private final AudioAIModelClient audioAIModelClient;
    private final BirdRepository birdRepository;
    private final SightingRepository sightingRepository;
    private final UserRepository userRepository;
    private final BucketService bucketService;
    private static final String CACHE = "public, max-age=31536000, immutable";

    public Detection analyze(BirdnetAnalyzeRequest req) {
        // 🔹 Ejecutar análisis principal (lógica original)
        BirdnetAnalyzeResponse resp = audioAIModelClient.analyze(req);

        Detection best = (resp == null || resp.getDetections() == null)
                ? null
                : resp.getDetections().stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparingDouble(Detection::getConfidence))
                .orElse(null);

        if (best == null) return defaultDetectionResponse();

        String raw = best.getLabel();
        if (raw == null || raw.isBlank()) return defaultDetectionResponse();

        String sci = extractScientific(raw);
        boolean ok = birdRepository.existsByNameIgnoreCase(sci);

        if (!ok) {
            ok = birdRepository.existsNormalized(sci);
        }
        if (!ok) {
            String common = extractCommon(raw);
            if (common != null) {
                ok = birdRepository.existsByCommonNameIgnoreCase(common)
                        || birdRepository.existsNormalized(common);
            }
        }

        if (ok) {
            best.setLabel(sci);
        }

        // 🟢 Si la detección es válida, crear el avistamiento y guardar el audio
        if (ok) {
            try {
                log.info("🎧 Creando SightingEntity en estado PENDING para usuario {}", req.getEmail());

                UserEntity user = userRepository.findByEmail(req.getEmail())
                        .orElseThrow(() -> new UserNotFoundException(req.getEmail()));

                // 1️⃣ Crear avistamiento pendiente
                SightingEntity pending = SightingEntity.builder()
                        .user(user)
                        .dateTime(LocalDateTime.now())
                        .state(SightingStatus.PENDING.name())
                        .build();

                pending = sightingRepository.save(pending);
                log.info("✅ SightingEntity creado con ID: {}", pending.getSightingId());

                // 2️⃣ Extraer datos y guardar el archivo de audio
                String mimeType = FileMetadataExtractor.extractMimeType(req.getBase64());
                byte[] data = FileMetadataExtractor.extractData(req.getBase64());

                String slugBird = Slugs.of(best.getLabel());
                String generated = FilenameGenerator.generate(
                        req.getEmail() != null ? req.getEmail() : "anonimo",
                        slugBird,
                        mimeType
                );

                String keyWithinBucket = String.format("%s/%s/%s/%s",
                        req.getEmail(),
                        slugBird,
                        pending.getSightingId(),
                        lastPathSegment(generated)
                );

                bucketService.uploadSightingObject(keyWithinBucket, data, toContentType(mimeType), CACHE);
                log.info("🎵 Audio guardado en bucket con key: {}", keyWithinBucket);

                // 3️⃣ Asociar el sightingId al resultado
                best.setSightingId(pending.getSightingId());

            } catch (Exception e) {
                log.error("❌ Error al guardar avistamiento o subir audio: {}", e.getMessage(), e);
            }
        }

        return ok ? best : defaultDetectionResponse();
    }

    // ---------------- utilitarios ----------------

    private String extractScientific(String label) {
        String left = splitLeft(label, "_", "-", "/", "(", ",");
        String[] parts = left.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0] + " " + parts[1]).trim();
        }
        return left.trim();
    }

    private String extractCommon(String label) {
        if (label == null) return null;
        int i = label.indexOf('_');
        if (i >= 0 && i + 1 < label.length()) {
            return label.substring(i + 1).trim();
        }
        return null;
    }

    private String splitLeft(String s, String... seps) {
        if (s == null) return "";
        int cut = s.length();
        for (String sep : seps) {
            int idx = s.indexOf(sep);
            if (idx >= 0) cut = Math.min(cut, idx);
        }
        return s.substring(0, cut);
    }

    private static String toContentType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) return "audio/wav";
        return mimeType;
    }

    private static String lastPathSegment(String path) {
        if (path == null || path.isBlank()) return "audio";
        int i = path.lastIndexOf('/');
        return (i >= 0 && i < path.length() - 1) ? path.substring(i + 1) : path;
    }
}
