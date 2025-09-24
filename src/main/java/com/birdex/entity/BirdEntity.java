package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Table(name = "birds")
public class BirdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bird_id")
    private UUID birdId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "common_name", nullable = false)
    private String commonName;

    @Column(name = "size", nullable = false)
    private String size;

    @Column(name = "length_min_mm")
    private Integer lengthMinMm;

    @Column(name = "length_max_mm")
    private Integer lengthMaxMm;

    @Column(name = "weight_min_g")
    private Integer weightMinG;

    @Column(name = "weight_max_g")
    private Integer weightMaxG;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "characteristics", nullable = false)
    private String characteristics;

    @Column(name = "image", nullable = false)
    private String image;

    @OneToMany(mappedBy = "bird", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<MigratoryWaveEntity> migratoryWaves = new HashSet<>();
}