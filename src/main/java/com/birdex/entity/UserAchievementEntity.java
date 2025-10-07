package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.birdex.utils.JsonbConverter;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "user_achievements",
        indexes = {
                @Index(name = "idx_user_achiev_user", columnList = "user_id"),
                @Index(name = "idx_user_achiev_ach", columnList = "achievement_id")
        })
public class UserAchievementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_achievement_id", nullable = false, updatable = false)
    private UUID userAchievementId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_id", nullable = false)
    private AchievementEntity achievement;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "progress", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private Map<String, Object> progress = new HashMap<>();

    @Column(name = "obtained_at")
    @Builder.Default
    private LocalDateTime obtainedAt = LocalDateTime.now();
}
