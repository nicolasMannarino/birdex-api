package com.birdex.entity;


import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "colors", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@Builder
public class ColorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "color_id")
    private UUID colorId;

    @Column(name = "name", nullable = false)
    private String name;
}
