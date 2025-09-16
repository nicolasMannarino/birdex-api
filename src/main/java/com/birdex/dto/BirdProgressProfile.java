package com.birdex.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BirdProgressProfile {
    private String photoBase64;
    private String rarity;
    private String name;
    private String commonName;
}
