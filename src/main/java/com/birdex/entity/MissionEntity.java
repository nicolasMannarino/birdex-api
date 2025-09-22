package com.birdex.entity;

import com.birdex.entity.enums.MissionType;
import com.birdex.utils.JsonbConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "missions")
public class MissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "mission_id", nullable = false, updatable = false)
    private UUID missionId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "type", nullable = false)
    private String typeDb;

    @Transient
    public MissionType getType() {
        return MissionType.fromDb(typeDb);
    }

    public void setType(MissionType type) {
        this.typeDb = (type == null) ? MissionType.UNIQUE.toDb() : type.toDb();
    }

    @Convert(converter = JsonbConverter.class)
    @Column(name = "objective", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> objective;

    @Column(name = "reward_points", nullable = false)
    @Builder.Default
    private Integer rewardPoints = 0;
}
