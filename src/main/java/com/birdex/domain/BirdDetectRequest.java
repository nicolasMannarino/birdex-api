package com.birdex.domain;


import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class BirdDetectRequest {
    private String fileBase64;
}
