package com.birdex.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SightingFullResponse {
    private UUID sightingId;
    private String birdName;
    private String commonName;
    private String rarity;
    private String location;
    private LocalDateTime dateTime;
    private String userEmail;
    private String username;
    private List<String> imagesBase64;
}