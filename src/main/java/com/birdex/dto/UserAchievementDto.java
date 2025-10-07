package com.birdex.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAchievementDto {
    private UUID userAchievementId;
    private String achievementName;
    private String description;
    private Map<String, Object> criteria;
    private Map<String, Object> progress;
    private String iconUrl;
    private LocalDateTime obtainedAt;
}
