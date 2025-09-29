package com.birdex.client;

import com.birdex.domain.BirdnetAnalyzeRequest;
import com.birdex.domain.BirdnetAnalyzeResponse;
import com.birdex.domain.Detection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static com.birdex.utils.DefaultResponse.defaultResponse;

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
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(BirdnetAnalyzeResponse.class);
                    } else {
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .doOnNext(body -> log.warn("BirdNET error {}: {}", clientResponse.statusCode(), body))
                                .thenReturn(defaultResponse());
                    }
                })
                .onErrorResume(ex -> {
                    log.error("BirdNET call failed: {}", ex.getMessage(), ex);
                    return reactor.core.publisher.Mono.just(defaultResponse());
                })
                .block();

        response = coalesce(response);
        log.info("Response: [{}]", response);
        return response;
    }

    private BirdnetAnalyzeResponse coalesce(BirdnetAnalyzeResponse r) {
        if (r == null || r.getDetections() == null || r.getDetections().isEmpty()) {
            return defaultResponse();
        }
        return r;
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
