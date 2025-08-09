package com.birdex.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BirdnetAnalyzeRequest {
    private String audioBase64;
    private Double minConf;
}
