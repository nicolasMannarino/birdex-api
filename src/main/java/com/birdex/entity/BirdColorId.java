package com.birdex.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
public class BirdColorId implements Serializable {
    private UUID birdId;
    private UUID colorId;
}
