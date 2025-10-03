package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "zones", uniqueConstraints = {
        @UniqueConstraint(name = "uk_zones_name", columnNames = "name")
})
public class ZoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "zone_id", nullable = false, updatable = false)
    private UUID zoneId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZoneEntity that)) return false;
        return zoneId != null && zoneId.equals(that.zoneId);
    }

    @Override
    public int hashCode() {
        return ZoneEntity.class.hashCode();
    }
}
