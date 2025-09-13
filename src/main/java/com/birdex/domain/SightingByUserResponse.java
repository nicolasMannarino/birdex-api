package com.birdex.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SightingByUserResponse {
    private List<SightingResponse> sightingResponseList;

}
