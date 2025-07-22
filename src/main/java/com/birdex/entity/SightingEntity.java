package com.birdex.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

@Entity
@Data
@Builder
@Table(name = "sightings")
public class SightingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String sightingID;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "dateTime", nullable = false)
    private LocalDateTime dateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bird_id", nullable = false)
    private BirdEntity bird;
}