package com.birdex.entity;

import java.util.UUID;

public interface BirdSummary {
    UUID getBirdId();
    String getName();
    String getCommonName();
    String getImage();
    String getSize();
}
