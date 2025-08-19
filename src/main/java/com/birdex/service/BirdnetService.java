package com.birdex.service;

import com.birdex.client.AudioAIModelClient;
import com.birdex.domain.BirdnetAnalyzeRequest;
import com.birdex.domain.BirdnetAnalyzeResponse;
import com.birdex.domain.Detection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
@Slf4j
public class BirdnetService {
    private final AudioAIModelClient audioAIModelClient;

    public BirdnetService(AudioAIModelClient audioAIModelClient) {
        this.audioAIModelClient = audioAIModelClient;
    }

    public Detection analyze(BirdnetAnalyzeRequest req) {
        BirdnetAnalyzeResponse birdnetAnalyzeResponse = audioAIModelClient.analyze(req);

        return birdnetAnalyzeResponse.getDetections().stream()
                .max(Comparator.comparingDouble(Detection::getConfidence))
                .orElse(null);
    }


}
