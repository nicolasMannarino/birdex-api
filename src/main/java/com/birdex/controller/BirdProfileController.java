package com.birdex.controller;

import com.birdex.config.BucketProperties;
import com.birdex.service.BucketService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/birds")
@RequiredArgsConstructor
public class BirdProfileController {

    private final BucketService bucketService;
    private final BucketProperties props;


    @PostMapping("/{birdName}/profile")
    public ResponseEntity<Map<String, String>> uploadProfileBase64(
            @PathVariable String birdName,
            @RequestBody BirdProfileBase64Request body
    ) {
        String key = bucketService.putBirdProfileBase64(
                birdName,
                body.getBase64(),
                body.getContentType()
        );

        String bucket = props.getBirds().getBucket();
        String url = bucketService.buildPublicUrl(bucket, key);

        return ResponseEntity.ok(Map.of(
                "bucket", bucket,
                "key", key,
                "url", url
        ));
    }


    @GetMapping("/{birdName}/profile/base64")
    public ResponseEntity<Map<String, String>> getProfileBase64(@PathVariable String birdName) {
        String base64 = bucketService.getBirdProfileBase64(birdName);
        return ResponseEntity.ok(Map.of("base64", base64));
    }


    @GetMapping("/{birdName}/profile/data-uri")
    public ResponseEntity<Map<String, String>> getProfileDataUri(@PathVariable String birdName) {
        String dataUri = bucketService.getBirdProfileDataUri(birdName);
        return ResponseEntity.ok(Map.of("dataUri", dataUri));
    }

    @Data
    public static class BirdProfileBase64Request {
        private String base64;
        private String contentType;
    }
}
