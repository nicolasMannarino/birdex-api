package com.birdex.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.birdex.domain.SightingImageItem;

@Data @Builder
@Schema(name = "SightingWithImagesResponse", description = "Resumen de avistaje con imágenes")
public class SightingWithImagesResponse {

    @Schema(description = "Nombre científico", example = "Turdus rufiventris")
    private String birdName;

    @Schema(description = "Nombre común", example = "Zorzal colorado")
    private String commonName;

    @Schema(description = "Rareza", example = "Comun")
    private String rarity;

    @Schema(description = "Fecha/hora del avistaje", example = "2025-09-06T17:07:45")
    private LocalDateTime dateTime;

    @Schema(description = "Latitud", example = "-34.603722")
    private BigDecimal latitude;

    @Schema(description = "Longitud", example = "-58.381592")
    private BigDecimal longitude;

    @ArraySchema(arraySchema = @Schema(description = "Imágenes (thumb + image) del avistaje"))
    @Builder.Default
    private List<SightingImageItem> images = new ArrayList<>();
}