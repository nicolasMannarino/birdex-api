package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "levels")
public class LevelEntity {

    @Id
    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "xp_required", nullable = false)
    private Integer xpRequired;
}
