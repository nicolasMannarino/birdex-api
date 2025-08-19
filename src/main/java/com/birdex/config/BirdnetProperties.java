package com.birdex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "birdnet.api")
public class BirdnetProperties {
    private String baseUrl;
    private String timeoutSeconds;
}
