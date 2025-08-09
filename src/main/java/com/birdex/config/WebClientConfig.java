package com.birdex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class WebClientConfig {

    private final BirdnetProperties birdnetProperties;

    public WebClientConfig(BirdnetProperties birdnetProperties) {
        this.birdnetProperties = birdnetProperties;
    }

    @Bean
    public WebClient birdnetWebClient(BirdnetProperties birdnetProperties) {

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(java.time.Duration.ofSeconds(Long.parseLong(birdnetProperties.getTimeoutSeconds())));

        return WebClient.builder()
                .baseUrl(birdnetProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
