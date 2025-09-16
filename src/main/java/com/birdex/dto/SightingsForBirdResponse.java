package com.birdex.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SightingsForBirdResponse {
    private String birdName;
    private String commonName;
    private String rarity;
    private String profilePhotoBase64;

    private List<SightingFullResponse> mine;
    private List<SightingFullResponse> others;
}
