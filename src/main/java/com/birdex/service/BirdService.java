package com.birdex.service;

import com.birdex.dto.BirdDto;
import com.birdex.dto.BirdProgressProfile;
import com.birdex.dto.BirdProgressResponse;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.BirdNamesView;
import com.birdex.entity.BirdSummary;
import com.birdex.mapper.BirdMapper;
import com.birdex.repository.BirdRarityRepository;
import com.birdex.repository.BirdRepository;
import com.birdex.utils.Slugs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BirdService {
    private final BirdRepository birdRepository;
    private final BirdMapper birdMapper;
    private final BucketService bucketService;
    private final BirdRarityRepository birdRarityRepository;


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

        BirdDto dto = birdMapper.toDto(entity);
        String image = bucketService.getBirdProfileBase64(dto.getName());

        dto.setImage(image);

        return dto;
    }

    public BirdProgressResponse getBirds() {
        List<BirdNamesView> birds = birdRepository.findAllByOrderByNameAsc();

        List<BirdProgressProfile> list = new ArrayList<>();
        BirdProgressResponse.BirdProgressResponseBuilder builder = BirdProgressResponse.builder();

        for (BirdNamesView b : birds) {
            String slugifyName = Slugs.of(b.getName());
            String birdPhoto = bucketService.getBirdProfileBase64(slugifyName);
            String rarity = birdRarityRepository.findRarityNameByBirdName(b.getName()).orElse("");

            BirdProgressProfile bpp = BirdProgressProfile.builder()
                    .name(b.getName())
                    .rarity(rarity)
                    .commonName(b.getCommonName())
                    .photoBase64(birdPhoto)
                    .build();

            list.add(bpp);
        }

        return builder
                .list(list)
                .build();
    }

    public Page<BirdSummary> search(String rarity, String color, String size, Pageable pageable) {
        String r = normalize(rarity);
        String c = normalize(color);
        String s = normalize(size);

        return birdRepository.searchBirds(r, c, s, pageable);
    }

    private String normalize(String v) {
        return StringUtils.hasText(v) ? v.trim() : null;
    }

}
