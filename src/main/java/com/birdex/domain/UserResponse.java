package com.birdex.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
@Schema(name = "UserResponse", description = "Resumen de usuario")
public class UserResponse {
    private String username;
    private String email;
    private Integer points;
    private Integer level;
    private String levelName;
    private String profilePhotoUrl;
    private String profilePhotoThumbUrl;
    private Integer xpRequired;
}
