package com.birdex.service;

import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import com.birdex.domain.BirdVideoDetectRequest;
import com.birdex.domain.BirdVideoDetectResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DetectionService {
    private final ModelProcessor modelProcessor;

    public DetectionService(ModelProcessor modelProcessor) {
        this.modelProcessor = modelProcessor;
    }

    public BirdDetectResponse detect(MultipartFile file) {
        return modelProcessor.evaluateImage(file);
    }

    public BirdVideoDetectResponse detectVideo(MultipartFile file, int sampleFps, boolean stopOnFirstAbove) {
        return modelProcessor.evaluateVideo(file, sampleFps, stopOnFirstAbove);
    }
}
