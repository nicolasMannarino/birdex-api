package com.birdex.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder
@Schema(name = "SightingFullResponse", description = "Detalle completo de un avistaje")
public class SightingFullResponse {

    @Schema(description = "ID del avistaje", example = "de305d54-75b4-431b-adb2-eb6b9e546014")
    private UUID sightingId;

    @Schema(description = "Nombre científico", example = "Turdus rufiventris")
    private String birdName;

    @Schema(description = "Nombre común", example = "Zorzal colorado")
    private String commonName;

    @Schema(description = "Rareza (p. ej. Comun, Poco comun, Raro, Epico, Legendario)", example = "Comun")
    private String rarity;

    @Schema(description = "Ubicación", example = "Parque Saavedra, CABA")
    private String location;

    @Schema(description = "Fecha/hora", example = "2025-09-06T17:07:45")
    private LocalDateTime dateTime;

    @Schema(description = "Email del usuario", example = "user@example.com")
    private String userEmail;

    @Schema(description = "Nombre de usuario", example = "federico")
    private String username;

    @ArraySchema(arraySchema = @Schema(description = "Imágenes del avistaje (base64)"))
    private List<String> imagesBase64;
}