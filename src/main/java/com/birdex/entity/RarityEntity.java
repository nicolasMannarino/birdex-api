package com.birdex.entity;


import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "rarities", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@Builder
public class RarityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rarity_id")
    private UUID rarityId;

    @Column(name = "name", nullable = false)
    private String name;
}
