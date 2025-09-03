package com.birdex.service;

import com.birdex.dto.BirdDto;
import com.birdex.entity.BirdEntity;
import com.birdex.mapper.BirdMapper;
import com.birdex.repository.BirdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BirdService {
    private final BirdRepository birdRepository;
    private final BirdMapper birdMapper;


    public String getDescription(String commonName) {
        return birdRepository.findFirstByCommonNameContainingIgnoreCase(commonName).orElseThrow(() -> {
            log.warn("No bird found for common name: {}", commonName);
            return new RuntimeException("Bird not found for common name: " + commonName);
        }).getDescription();
    }

    public BirdDto getBySpecificName(String specificName){
        BirdEntity entity = birdRepository.findFirstByNameContainingIgnoreCase(specificName).orElseThrow(() -> {
            log.warn("No bird found for name: {}", specificName);
            return new RuntimeException("Bird not found for name: " + specificName);
        });

        return birdMapper.toDto(entity);
    }
}
