package com.birdex.mapper;

import com.birdex.dto.SightingDto;
import com.birdex.entity.BirdEntity;
import com.birdex.entity.SightingEntity;
import com.birdex.entity.UserEntity;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SightingMapper {

    private SightingMapper() {
    }

    public static SightingDto toDto(SightingEntity s) {
        if (s == null) return null;

        BirdEntity b = s.getBird();
        UserEntity u = s.getUser();

        String location = formatLocation(s.getLatitude(), s.getLongitude(), s.getLocationText());

        return new SightingDto(
                location,
                s.getDateTime(),
                b != null ? b.getName() : null,
                b != null ? b.getCommonName() : null,
                u != null ? u.getEmail() : null,
                s.getLatitude(),
                s.getLongitude()
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

    private static String formatLocation(BigDecimal lat, BigDecimal lon, String locationText) {
        if (lat == null || lon == null) return null;
        String latLon = lat.stripTrailingZeros().toPlainString() + "," + lon.stripTrailingZeros().toPlainString();
        if (locationText != null && !locationText.isBlank()) {
            return latLon + " (" + locationText.trim() + ")";
        }
        return latLon;
    }
}
