package com.birdex.mapper;

import com.birdex.entity.BirdEntity;
import com.birdex.dto.BirdDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface BirdMapper {

    @Mapping(target = "birdId", ignore = true)
    BirdDto toDto(BirdEntity entity);


    @Mapping(target = "birdId", ignore = true)
    BirdEntity toEntity(BirdDto dto);
}
