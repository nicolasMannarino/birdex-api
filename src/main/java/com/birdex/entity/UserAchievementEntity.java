package com.birdex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(name = "obtained_at")
    @Builder.Default
    private LocalDateTime obtainedAt = LocalDateTime.now();
}
