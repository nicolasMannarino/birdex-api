package com.birdex.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BirdnetAnalyzeRequest {
    @NotBlank(message = "audio_base64 is required and cannot be empty")
    @JsonProperty("audio_base64")
    private String base64;

    @JsonProperty("min_conf")
    private Double minConf = 0.8;
}
