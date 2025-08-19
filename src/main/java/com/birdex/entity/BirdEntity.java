package com.birdex.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "birds")
@Builder
@Data
public class BirdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bird_id")
    private UUID birdID;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "common_name", nullable = false)
    private String commonName;

    @Column(name = "size", nullable = false)
    private String size;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "characteristics", nullable = false, columnDefinition = "text")
    private String characteristics;

    @Column(name = "image", nullable = false)
    private String image;

    @Column(name = "migratory_wave_url", nullable = false)
    private String migratoryWaveUrl;

    @OneToMany(mappedBy = "bird", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Set<BirdColor> colors = new HashSet<>();

    @OneToMany(mappedBy = "bird", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Set<BirdRarityEntity> rarities = new HashSet<>();
}
