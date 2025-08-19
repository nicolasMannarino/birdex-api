package com.birdex.service;

import com.birdex.repository.BirdRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BirdService {
    private final BirdRepository birdRepository;

    public BirdService(BirdRepository birdRepository) {
        this.birdRepository = birdRepository;
    }

    public String getDescription(String commonName) {
        return birdRepository.findByCommonNameLike(commonName).orElseThrow(() -> {
            log.warn("No bird found for common name: {}", commonName);
            return new RuntimeException("Bird not found for common name: " + commonName);
        }).getDescription();
    }
}
