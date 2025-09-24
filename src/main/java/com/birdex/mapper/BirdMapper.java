package com.birdex.mapper;


import com.birdex.dto.BirdDto;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.MigratoryWaveEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class BirdMapper {

    public BirdDto toDto(BirdEntity entity) {
        if (entity == null) return null;

        BirdDto dto = new BirdDto();
        dto.setName(entity.getName());
        dto.setCommonName(entity.getCommonName());
        dto.setSize(entity.getSize());
        dto.setDescription(entity.getDescription());
        dto.setCharacteristics(entity.getCharacteristics());
        dto.setImage(entity.getImage());

        dto.setSizeDetails(
                BirdDto.Measurements.builder()
                        .lengthMinMm(entity.getLengthMinMm())
                        .lengthMaxMm(entity.getLengthMaxMm())
                        .weightMinG(entity.getWeightMinG())
                        .weightMaxG(entity.getWeightMaxG())
                        .build()
        );

        if (entity.getMigratoryWaves() != null && !entity.getMigratoryWaves().isEmpty()) {
            Map<Short, List<String>> wave = entity.getMigratoryWaves().stream()
                    .collect(Collectors.groupingBy(
                            MigratoryWaveEntity::getMonth,
                            TreeMap::new,
                            Collectors.mapping(
                                    mw -> mw.getProvince().getName(),
                                    Collectors.collectingAndThen(
                                            Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)),
                                            ArrayList::new
                                    )
                            )
                    ));
            dto.setMigratoryWave(wave);
        } else {
            dto.setMigratoryWave(Collections.emptyMap());
        }

        return dto;
    }

    public BirdEntity toEntity(BirdDto dto) {
        if (dto == null) return null;

        BirdEntity entity = new BirdEntity();
        entity.setName(dto.getName());
        entity.setCommonName(dto.getCommonName());
        entity.setSize(dto.getSize());

        if (dto.getSizeDetails() != null) {
            entity.setLengthMinMm(dto.getSizeDetails().getLengthMinMm());
            entity.setLengthMaxMm(dto.getSizeDetails().getLengthMaxMm());
            entity.setWeightMinG(dto.getSizeDetails().getWeightMinG());
            entity.setWeightMaxG(dto.getSizeDetails().getWeightMaxG());
        }

        entity.setDescription(dto.getDescription());
        entity.setCharacteristics(dto.getCharacteristics());
        entity.setImage(dto.getImage());
        return entity;
    }
}