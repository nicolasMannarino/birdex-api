package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bird_color",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bird_id", "color_id"}))
@Data
@Builder
public class BirdColor {

    @EmbeddedId
    private BirdColorId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("birdId")
    @JoinColumn(name = "bird_id", nullable = false)
    private BirdEntity bird;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("colorId")
    @JoinColumn(name = "color_id", nullable = false)
    private ColorEntity color;

}
