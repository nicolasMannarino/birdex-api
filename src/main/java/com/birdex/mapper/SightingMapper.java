package com.birdex.mapper;

import com.birdex.dto.SightingDto;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SightingMapper {

    private SightingMapper() {}

    public static SightingDto toDto(SightingEntity s) {
        if (s == null) return null;

        BirdEntity b = s.getBird();
        UserEntity u = s.getUser();

        return new SightingDto(
                s.getLocation(),
                s.getDateTime(),
                b != null ? b.getName() : null,
                b != null ? b.getCommonName() : null,
                u != null ? u.getEmail() : null
        );
    }

    public static List<SightingDto> toDtoList(Collection<SightingEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .filter(Objects::nonNull)
                .map(SightingMapper::toDto)
                .toList();
    }

    public static Optional<SightingDto> toDto(Optional<SightingEntity> entityOpt) {
        return entityOpt.map(SightingMapper::toDto);
    }
}
