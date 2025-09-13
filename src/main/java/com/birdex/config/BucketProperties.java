package com.birdex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class BucketProperties {
    private String bucket;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region;
    private Birds birds;


    @Data
    public static class Birds {
        private String bucket;
        private String profileObjectName = "profile";
    }
}
