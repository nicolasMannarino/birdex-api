package com.birdex.dto;

import com.birdex.entity.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportAdminDto(
        UUID id,
        UUID sightingId,
        String reportedByEmail,
        String description,
        ReportStatus status,
        LocalDateTime reportedAt,
        LocalDateTime readAt
) {}
