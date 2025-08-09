package com.birdex.service;

import com.birdex.domain.BirdnetAnalyzeRequest;
import com.birdex.domain.BirdnetAnalyzeResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class BirdnetService {
    private final WebClient webClient;

    public BirdnetService(WebClient birdnetWebClient) {
        this.webClient = birdnetWebClient;
    }

    public BirdnetAnalyzeResponse analyze(BirdnetAnalyzeRequest req) {
        return webClient.post()
                .uri("/analyze")
                .bodyValue(req)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        resp -> resp.bodyToMono(String.class)
                                .map(body -> new RuntimeException("BirdNET error: " + body))
                )
                .bodyToMono(BirdnetAnalyzeResponse.class)
                .block();
    }

    public boolean health() {
        try {
            var m = webClient.get().uri("/health")
                    .retrieve().bodyToMono(Map.class).block();
            return m != null && "ok".equals(m.get("status"));
        } catch (Exception e) {
            return false;
        }
    }
}
