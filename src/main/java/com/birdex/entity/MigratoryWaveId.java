package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class MigratoryWaveId implements Serializable {
    @Column(name = "bird_id", nullable = false)
    private UUID birdId;

    @Column(name = "month", nullable = false)
    private Short month; // 1..12

    @Column(name = "province_id", nullable = false)
    private UUID provinceId;
}
