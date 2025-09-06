package com.birdex.dto;

import java.time.LocalDateTime;

public record SightingDto(
        String location,
        LocalDateTime dateTime,
        String birdName,
        String birdCommonName,
        String userEmail
) {}