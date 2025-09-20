package com.birdex.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

import java.math.BigDecimal;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "sightings",
        indexes = {
                @Index(name = "idx_sightings_lat_lon", columnList = "latitude, longitude"),
                @Index(name = "idx_sightings_datetime", columnList = "\"dateTime\""),
                @Index(name = "idx_sightings_user", columnList = "user_id"),
                @Index(name = "idx_sightings_bird", columnList = "bird_id")
        }
)
public class SightingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sighting_id", nullable = false, updatable = false)
    private UUID sightingId;

    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "location_text")
    private String locationText;

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bird_id", nullable = false)
    private BirdEntity bird;
}