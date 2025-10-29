package com.birdex.domain;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BirdVideoDetectResponse {

    @JsonAlias({"bestLabel"})
    private String label;

    @JsonAlias({"bestConfidence"})
    private double trustLevel;

    private UUID sightingId;
}
