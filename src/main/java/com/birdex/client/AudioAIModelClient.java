package com.birdex.client;

import com.birdex.domain.BirdnetAnalyzeRequest;
import com.birdex.domain.BirdnetAnalyzeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class AudioAIModelClient {

    private final WebClient webClient;

    public AudioAIModelClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public BirdnetAnalyzeResponse analyze(BirdnetAnalyzeRequest req) {
        log.info("Analyzing audio...");
        BirdnetAnalyzeResponse response = webClient.post()
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
        log.info("Response: [" + response + "]");
        return response;
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
