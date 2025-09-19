package com.birdex.entity;


import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "provinces", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProvinceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "province_id")
    private UUID provinceId;

    @Column(name = "name", nullable = false)
    private String name;
}
