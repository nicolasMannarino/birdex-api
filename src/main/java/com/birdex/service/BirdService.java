package com.birdex.service;

import com.birdex.dto.BirdDto;
import com.birdex.dto.BirdProgressProfile;
import com.birdex.dto.BirdProgressResponse;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.BirdNamesView;
import com.birdex.mapper.BirdMapper;
import com.birdex.repository.BirdRepository;
import com.birdex.utils.Slugs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BirdService {
    private final BirdRepository birdRepository;
    private final BirdMapper birdMapper;
    private final BucketService bucketService;


    public String getDescription(String commonName) {
        return birdRepository.findFirstByCommonNameContainingIgnoreCase(commonName).orElseThrow(() -> {
            log.warn("No bird found for common name: {}", commonName);
            return new RuntimeException("Bird not found for common name: " + commonName);
        }).getDescription();
    }

    public BirdDto getBySpecificName(String specificName) {
        BirdEntity entity = birdRepository.findFirstByNameContainingIgnoreCase(specificName).orElseThrow(() -> {
            log.warn("No bird found for name: {}", specificName);
            return new RuntimeException("Bird not found for name: " + specificName);
        });

        return birdMapper.toDto(entity);
    }

    public BirdProgressResponse getBirds() {
        List<BirdNamesView> birds = birdRepository.findAllByOrderByNameAsc();

        List<BirdProgressProfile> list = new ArrayList<>();
        BirdProgressResponse.BirdProgressResponseBuilder builder = BirdProgressResponse.builder();

        for (BirdNamesView b : birds) {
            String slugifyName = Slugs.of(b.getName());
            String birdPhoto = bucketService.getBirdProfileBase64(slugifyName);

            BirdProgressProfile bpp = BirdProgressProfile.builder()
                    .name(b.getName())
                    .commonName(b.getCommonName())
                    .photoBase64(birdPhoto)
                    .build();

            list.add(bpp);
        }

        return builder
                .list(list)
                .build();
    }
}
