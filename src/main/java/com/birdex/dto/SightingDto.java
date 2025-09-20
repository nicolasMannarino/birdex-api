package com.birdex.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SightingDto(
        String location,
        LocalDateTime dateTime,
        String birdName,
        String birdCommonName,
        String userEmail,
        BigDecimal latitude,
        BigDecimal longitude
) {
}