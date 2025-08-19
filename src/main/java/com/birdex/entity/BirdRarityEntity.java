package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bird_rarity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bird_id", "rarity_id"}))
@Data
@Builder
public class BirdRarityEntity {

    @EmbeddedId
    private BirdRarityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("birdId")
    @JoinColumn(name = "bird_id", nullable = false)
    private BirdEntity bird;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("rarityId")
    @JoinColumn(name = "rarity_id", nullable = false)
    private RarityEntity rarity;
}