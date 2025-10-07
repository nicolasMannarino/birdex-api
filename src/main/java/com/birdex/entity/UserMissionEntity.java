package com.birdex.entity;

import com.birdex.utils.JsonbConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;
import java.util.HashMap;
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


        public UserMissionEntity(UserEntity user, MissionEntity mission, int initialProgress, boolean completed) {
                this.user = user;
                this.mission = mission;
                this.progress = new HashMap<>();
                this.progress.put("count", initialProgress);
                this.completed = completed;
        }

        public int getProgressValue() {
                if (progress == null) return 0;
                Object val = progress.get("count");
                if (val instanceof Number) {
                return ((Number) val).intValue();
                }
                return 0;
        }

        public void setProgressValue(int value) {
                if (progress == null) {
                progress = new HashMap<>();
                } else if (!(progress instanceof HashMap)) {
                progress = new HashMap<>(progress); // por si venía como Map inmutable
                }
                progress.put("count", value);
        }

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
    @ColumnTransformer(write = "?::jsonb")
    private Map<String, Object> progress = new HashMap<>();

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
