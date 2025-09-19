package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "migratory_waves")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MigratoryWaveEntity {

    @EmbeddedId
    private MigratoryWaveId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("birdId")
    @JoinColumn(name = "bird_id", nullable = false)
    private BirdEntity bird;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("provinceId")
    @JoinColumn(name = "province_id", nullable = false)
    private ProvinceEntity province;

    @Column(name = "month", insertable = false, updatable = false, nullable = false)
    private Short month;
}

