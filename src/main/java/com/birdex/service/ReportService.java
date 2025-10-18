package com.birdex.service;

import com.birdex.domain.SaveReportRequest;
import com.birdex.entity.ReportEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;
import com.birdex.entity.enums.ReportStatus;
import com.birdex.exception.UserNotFoundException;
import com.birdex.repository.ReportRepository;
import com.birdex.repository.SightingRepository;
import com.birdex.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {
    private final SightingRepository sightingRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    private static final String MSG_USER_NOT_FOUND_BY_EMAIL = "No encontramos una cuenta registrada con ese correo.";

    public UUID reportSave(SaveReportRequest request) {

        if (reportRepository.existsBySighting_SightingId(UUID.fromString(request.getSightingId()))) {
            throw new RuntimeException("Ya existe un reporte para este avistamiento");
        }

        SightingEntity sighting = sightingRepository.findById(UUID.fromString(request.getSightingId()))
                .orElseThrow(() -> new RuntimeException("Avistamiento no encontrado"));

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(MSG_USER_NOT_FOUND_BY_EMAIL));

        ReportEntity toSave = ReportEntity.builder()
                .reportedBy(user)
                .description(request.getDescription())
                .reportedAt(LocalDateTime.now())
                .status(ReportStatus.PENDING)
                .sighting(sighting)
                .build();

        ReportEntity response = reportRepository.save(toSave);

        return response.getId();

    }
}
