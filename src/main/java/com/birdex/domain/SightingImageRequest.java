package com.birdex.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SightingImageRequest {
    private String email;
    private String birdName;
}
