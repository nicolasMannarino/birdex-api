package com.birdex.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
@Schema(name = "SightingResponse", description = "Resumen de avistaje")
public class SightingResponse {

    @Schema(description = "Nombre científico", example = "Turdus rufiventris")
    private String birdName;

    @Schema(description = "Nombre común", example = "Zorzal colorado")
    private String commonName;

    @Schema(description = "Foto de perfil del ave en base64", example = "data:image/jpeg;base64,/9j/4AAQ...")
    private String profilePhotoBase64;

    @Schema(description = "Rareza (p. ej. Comun, Poco comun, Raro, Epico, Legendario)", example = "Comun")
    private String rarity;

    @Schema(description = "Fecha/hora del avistaje", example = "2025-09-06T17:07:45")
    private LocalDateTime dateTime;
}
