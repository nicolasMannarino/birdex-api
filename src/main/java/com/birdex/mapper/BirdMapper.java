package com.birdex.mapper;

import com.birdex.entity.BirdEntity;
import com.birdex.dto.BirdDto;
import org.mapstruct.*;
import org.springframework.stereotype.Component;


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
        dto.setMigratoryWaveUrl(entity.getMigratoryWaveUrl());
        return dto;
    }

    public BirdEntity toEntity(BirdDto dto) {
        if (dto == null) return null;

        BirdEntity entity = new BirdEntity();

        entity.setName(dto.getName());
        entity.setCommonName(dto.getCommonName());
        entity.setSize(dto.getSize());
        entity.setDescription(dto.getDescription());
        entity.setCharacteristics(dto.getCharacteristics());
        entity.setImage(dto.getImage());
        entity.setMigratoryWaveUrl(dto.getMigratoryWaveUrl());
        return entity;
    }
}
