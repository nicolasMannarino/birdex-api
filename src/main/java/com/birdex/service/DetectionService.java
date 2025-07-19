package com.birdex.service;

import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DetectionService {
    private final ModelProcessor modelProcessor;

    public DetectionService(ModelProcessor modelProcessor) {
        this.modelProcessor = modelProcessor;
    }

    public BirdDetectResponse detect(BirdDetectRequest request) {
        try {
            return modelProcessor.evaluate(request.getFileBase64());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
