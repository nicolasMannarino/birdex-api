package com.birdex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class BucketProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region;

    private Birds birds;
    private Sightings sightings;
    private Users users;

    @Data public static class Birds {
        private String bucket;
        private String profileObjectName;
    }
    @Data public static class Sightings {
        private String bucket;
    }

    @Data public static class Users {
        private String bucket;
    }
}
