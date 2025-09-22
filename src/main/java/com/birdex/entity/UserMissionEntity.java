package com.birdex.entity;

import com.birdex.utils.JsonbConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "user_missions",
        indexes = {
                @Index(name = "idx_user_missions_user", columnList = "user_id"),
                @Index(name = "idx_user_missions_mission", columnList = "mission_id")
        })
public class UserMissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_mission_id", nullable = false, updatable = false)
    private UUID userMissionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private MissionEntity mission;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "progress", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> progress = Map.of();

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
