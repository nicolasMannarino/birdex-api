package com.birdex.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "birds")
public class BirdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String birdID;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "commonName", nullable = false)
    private String commonName;

    @Column(name = "size", nullable = false)
    private String size;

    @Column(name = "color", nullable = false)
    private String color;

}
