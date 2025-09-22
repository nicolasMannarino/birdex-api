package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "rarity_points")
public class RarityPointsEntity {

    @Id
    @Column(name = "rarity_id", nullable = false, updatable = false)
    private UUID rarityId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "rarity_id", nullable = false)
    private RarityEntity rarity;

    @Column(name = "points", nullable = false)
    private Integer points;
}
