package com.birdex.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
@Schema(name = "BirdnetAnalyzeRequest", description = "Payload para analizar audio con BirdNET")
public class BirdnetAnalyzeRequest {

    @NotBlank(message = "audio_base64 is required and cannot be empty")
    @JsonProperty("audio_base64")
    @Schema(
            description = "Audio en base64 (con o sin data URI). Se recomienda WAV o MP3",
            example = "data:audio/wav;base64,UklGRiQAAABXQVZFZm10..."
    )
    private String base64;

    @JsonProperty("min_conf")
    @Schema(
            description = "Confianza mínima (0..1) para filtrar detecciones",
            example = "0.8",
            defaultValue = "0.8"
    )
    private Double minConf = 0.8;

    @NotBlank(message = "email is required")
    @JsonProperty("email")
    @Schema(
            description = "Correo electrónico del usuario que sube el audio",
            example = "usuario@example.com"
    )
    private String email;
}