package com.birdex.service;

import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import com.birdex.domain.BirdVideoDetectRequest;
import com.birdex.domain.BirdVideoDetectResponse;
import com.birdex.utils.Base64Sanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionService {

    private final ModelProcessor modelProcessor;

    public BirdDetectResponse detect(BirdDetectRequest req) {
        byte[] bytes = Base64Sanitizer.decode(req.getFileBase64());
        return modelProcessor.evaluateImage(bytes);
    }

    public BirdVideoDetectResponse detectVideo(BirdVideoDetectRequest req) {
        int fps = (req.getSampleFps() == null || req.getSampleFps() < 1) ? 1 : req.getSampleFps();
        boolean stop = req.getStopOnFirstAbove() != null && req.getStopOnFirstAbove();
        byte[] bytes = Base64Sanitizer.decode(req.getFileBase64());
        return modelProcessor.evaluateVideo(bytes, fps, stop);
    }
}