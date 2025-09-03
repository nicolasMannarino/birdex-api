package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "characteristics", nullable = false)
    private String characteristics;

    @Column(name = "image", nullable = false)
    private String image;

    @Column(name = "migratory_wave_url", nullable = false)
    private String migratoryWaveUrl;



}
