package com.birdex.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
@Schema(name = "SightingResponse", description = "Resumen de avistaje")
public class SightingResponse {

    @Schema(description = "Nombre científico", example = "Turdus rufiventris")
    private String birdName;

    @Schema(description = "Nombre común", example = "Zorzal colorado")
    private String commonName;

    @Schema(description = "Rareza (p. ej. Comun, Poco comun, Raro, Epico, Legendario)", example = "Comun")
    private String rarity;

    @Schema(description = "Fecha/hora del avistaje", example = "2025-09-06T17:07:45")
    private LocalDateTime dateTime;

    @Schema(description = "Latitud del avistaje", example = "-34.603722")
    private BigDecimal latitude;

    @Schema(description = "Longitud del avistaje", example = "-58.381592")
    private BigDecimal longitude;
}
