package com.birdex.domain;

import lombok.Data;

import java.util.List;

@Data
public class BirdnetAnalyzeResponse {
    private List<Detection> detections;
}
