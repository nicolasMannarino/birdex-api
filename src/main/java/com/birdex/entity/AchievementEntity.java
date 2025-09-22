package com.birdex.entity;

import com.birdex.utils.JsonbConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "achievements")
public class AchievementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "achievement_id", nullable = false, updatable = false)
    private UUID achievementId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Convert(converter = JsonbConverter.class)
    @Column(name = "criteria", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> criteria;

    @Column(name = "icon_url")
    private String iconUrl;
}
