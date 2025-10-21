package com.birdex.dto;

import com.birdex.domain.SightingImageItem;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
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

    @Schema(description = "Ubicación (texto o 'lat,lon (texto)')", example = "-34.587300,-58.416500 (Jardín Botánico Carlos Thays, CABA)")
    private String location;

    @Schema(description = "Fecha/hora", example = "2025-09-06T17:07:45")
    private LocalDateTime dateTime;

    @Schema(description = "Email del usuario", example = "user@example.com")
    private String userEmail;

    @Schema(description = "Nombre de usuario", example = "federico")
    private String username;

    @Schema(description = "URL pública de la imagen ‘cover’ (thumbnail)")
    private String coverThumbUrl;

    @Schema(description = "URL pública de la imagen ‘cover’ (600px)")
    private String coverImageUrl;

    @ArraySchema(arraySchema = @Schema(description = "Imágenes del avistaje (URLs públicas)", implementation = SightingImageItem.class))
    private List<SightingImageItem> images;

    @Schema(description = "Latitud", example = "-34.587300")
    private BigDecimal latitude;

    @Schema(description = "Longitud", example = "-58.416500")
    private BigDecimal longitude;
}
