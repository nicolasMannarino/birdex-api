package com.birdex.entity;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class BirdSummaryEnriched implements BirdSummary {
    private final UUID birdId;
    private final String name;
    private final String commonName;
    private final String image;
    private final String size;
    private final Integer lengthMinMm;
    private final Integer lengthMaxMm;
    private final Integer weightMinG;
    private final Integer weightMaxG;
    private final String rarity;
    private final List<String> colors;

    public BirdSummaryEnriched(BirdSummary src, String imageDataUri) {
        this.birdId = src.getBirdId();
        this.name = src.getName();
        this.commonName = src.getCommonName();
        this.image = imageDataUri;
        this.size = src.getSize();
        this.lengthMinMm = src.getLengthMinMm();
        this.lengthMaxMm = src.getLengthMaxMm();
        this.weightMinG = src.getWeightMinG();
        this.weightMaxG = src.getWeightMaxG();
        this.rarity = src.getRarity();
        this.colors = src.getColors();
    }
}
