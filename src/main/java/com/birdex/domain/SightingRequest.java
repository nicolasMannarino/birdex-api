package com.birdex.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(name = "SightingRequest", description = "Payload para registrar un avistaje")
public class SightingRequest {

    @Schema(description = "Imagen en base64 (con o sin data URI)", example = "data:image/jpeg;base64,/9j/4AAQ...")
    private String base64;

    @Schema(description = "Email del usuario", example = "user@example.com")
    private String email;

    @Schema(description = "Nombre científico del ave", example = "Turdus rufiventris")
    private String birdName;

    @Schema(description = "Latitud en grados decimales (-90..90)", example = "-34.603722")
    private BigDecimal latitude;

    @Schema(description = "Longitud en grados decimales (-180..180)", example = "-58.381592")
    private BigDecimal longitude;

    @Schema(description = "Descripción opcional del lugar (texto libre)", example = "Parque Saavedra, CABA")
    private String locationText;

    @Schema(description = "Ubicación libre o \"lat,lon\". DEPRECATED: preferir latitude/longitude",
            example = "-34.603722,-58.381592 (Obelisco, CABA)",
            deprecated = true)
    private String location;

    @Schema(description = "Fecha/hora local del evento", example = "2025-09-06T17:07:45")
    private LocalDateTime dateTime;
}
