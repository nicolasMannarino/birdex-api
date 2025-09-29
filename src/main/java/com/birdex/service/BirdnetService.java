package com.birdex.service;

import com.birdex.client.AudioAIModelClient;
import com.birdex.domain.BirdnetAnalyzeRequest;
import com.birdex.domain.BirdnetAnalyzeResponse;
import com.birdex.domain.Detection;
import com.birdex.entity.BirdEntity;
import com.birdex.repository.BirdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.birdex.utils.DefaultResponse.defaultDetectionResponse;
import static com.birdex.utils.DefaultResponse.defaultResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class BirdnetService {

    private final AudioAIModelClient audioAIModelClient;
    private final BirdRepository birdRepository;

    public Detection analyze(BirdnetAnalyzeRequest req) {
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

        return ok ? best : defaultDetectionResponse();
    }

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
}
