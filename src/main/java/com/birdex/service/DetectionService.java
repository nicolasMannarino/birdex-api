package com.birdex.service;

import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import com.birdex.domain.BirdVideoDetectRequest;
import com.birdex.domain.BirdVideoDetectResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DetectionService {
    private final ModelProcessor modelProcessor;

    public DetectionService(ModelProcessor modelProcessor) {
        this.modelProcessor = modelProcessor;
    }

    public BirdDetectResponse detect(BirdDetectRequest request) {
        return modelProcessor.evaluateImage(request.getFileBase64());
    }

    public BirdVideoDetectResponse detectVideo(BirdVideoDetectRequest request) {
        return modelProcessor.evaluateVideo(
                request.getFileBase64(),
                request.getSampleFps() != null ? request.getSampleFps() : 1,
                request.getStopOnFirstAbove() != null && request.getStopOnFirstAbove()
        );
    }
}
