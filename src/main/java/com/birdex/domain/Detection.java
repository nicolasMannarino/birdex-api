package com.birdex.domain;

import lombok.Data;

@Data
public class Detection {
    private double start_tim;
    private double end_time;
    private String label;
    private double confidence;
}