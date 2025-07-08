package com.birdex.controller;

import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/detect")
public class MockController {


    @PostMapping
    public ResponseEntity<BirdDetectResponse> mockResponse(@RequestBody BirdDetectRequest request) {
        String file = request.getFileBase64();

        BirdDetectResponse.BirdDetectResponseBuilder builder = new BirdDetectResponse.BirdDetectResponseBuilder();

        String mimeType = null;
        if (file != null && file.startsWith("data:")) {
            int semicolonIndex = file.indexOf(';');
            if (semicolonIndex > 5) {
                mimeType = file.substring(5, semicolonIndex);
            }
        }

        if (mimeType != null && mimeType.startsWith("image/")) {
            builder.label("Paroaria coronata");
            builder.trustLevel(0.9823);
        } else if (mimeType != null && mimeType.startsWith("video/")) {
            builder.label("Ave no identificada");
            builder.trustLevel(0.0);
        }

        builder.fileBase64(file);
        BirdDetectResponse response = builder.build();

        return ResponseEntity.ok(response);
    }
}
