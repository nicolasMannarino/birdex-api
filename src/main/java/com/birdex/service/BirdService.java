package com.birdex.service;

import com.birdex.dto.BirdDto;
import com.birdex.dto.BirdProgressProfile;
import com.birdex.dto.BirdProgressResponse;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.BirdNamesView;
import com.birdex.entity.BirdSummary;
import com.birdex.entity.BirdSummaryEnriched;
import com.birdex.mapper.BirdMapper;
import com.birdex.repository.BirdColorRepository;
import com.birdex.repository.BirdRarityRepository;
import com.birdex.repository.BirdRepository;
import com.birdex.utils.Slugs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final BirdColorRepository birdColorRepository;

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

        String rarity = birdRarityRepository.findRarityNameByBirdName(entity.getName()).orElse("");
        dto.setRarity(rarity);

        dto.setColors(birdColorRepository.findColorNamesByBirdName(entity.getName()));

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


    public Page<BirdSummary> search(
            String rarity,
            String color,
            String size,
            Integer lengthMinMm,
            Integer lengthMaxMm,
            Integer weightMinG,
            Integer weightMaxG,
            Pageable pageable
    ) {
        String r = normalize(rarity);
        String c = normalize(color);
        String s = normalize(size);

        Integer lenMin = cleanPositive(lengthMinMm);
        Integer lenMax = cleanPositive(lengthMaxMm);
        if (lenMin != null && lenMax != null && lenMin > lenMax) {
            int tmp = lenMin; lenMin = lenMax; lenMax = tmp;
        }

        Integer wMin = cleanPositive(weightMinG);
        Integer wMax = cleanPositive(weightMaxG);
        if (wMin != null && wMax != null && wMin > wMax) {
            int tmp = wMin; wMin = wMax; wMax = tmp;
        }

        Page<BirdSummary> page = birdRepository.searchBirds(r, c, s, lenMin, lenMax, wMin, wMax, pageable);

        List<BirdSummary> enriched = page.getContent().stream()
                .map(it -> {

                    String dataUri = bucketService.getBirdProfileDataUri(it.getName());

                    return new BirdSummaryEnriched(it, dataUri);
                })
                .map(BirdSummary.class::cast)
                .toList();

        return new PageImpl<>(enriched, pageable, page.getTotalElements());
    }

    private String normalize(String v) {
        return StringUtils.hasText(v) ? v.trim() : null;
    }

    private Integer cleanPositive(Integer v) {
        return (v != null && v > 0) ? v : null;
    }

    public String getRarityForBird(BirdEntity bird) {
        return birdRarityRepository.findRarityNameByBirdName(bird.getName())
                .orElse("COMMON"); // valor por defecto si no tiene rareza asignada
    }
}
