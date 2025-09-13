package com.birdex.domain;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SightingResponse {
    private String birdName;
    private String commonName;
    private String profilePhotoBase64;
    private String rarity;
    private LocalDateTime dateTime;
}
