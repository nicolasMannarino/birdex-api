package com.birdex.domain;

import lombok.Data;

@Data
public class SaveReportRequest {
    private String email;
    private String sightingId;
    private String description;
}
