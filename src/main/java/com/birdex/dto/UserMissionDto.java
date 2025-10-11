package com.birdex.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMissionDto {
    private UUID userMissionId;
    private String missionName;
    private String missionDescription;
    private String missionType;
    private Map<String, Object> objective;
    private Map<String, Object> progress;
    private boolean completed;
    private boolean claimed;
    private LocalDateTime completedAt;
    private Integer rewardPoints;
}