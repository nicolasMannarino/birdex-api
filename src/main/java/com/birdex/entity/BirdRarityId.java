package com.birdex.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class BirdRarityId implements Serializable {
    private UUID birdId;
    private UUID rarityId;
}