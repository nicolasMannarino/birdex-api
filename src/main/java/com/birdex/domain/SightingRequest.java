package com.birdex.domain;


import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SightingRequest {
    private String base64;
    private String email;
    private String birdName;
    private String location;
    private LocalDateTime dateTime;
}
