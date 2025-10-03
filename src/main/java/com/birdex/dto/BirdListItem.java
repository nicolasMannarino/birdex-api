package com.birdex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Schema(name = "BirdListItem", description = "Ítem de ave para listados/paginación")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirdListItem {

    @Schema(description = "ID", example = "6c8a0a7d-1cfc-4d13-9e4c-91b2d3f4b9d1")
    private UUID birdId;

    @Schema(description = "Nombre científico", example = "Turdus rufiventris")
    private String name;

    @Schema(description = "Nombre común", example = "Zorzal colorado")
    private String commonName;

    @Schema(description = "Tamaño (Grande, Muy grande, Mediano, Pequeño)", example = "Grande")
    private String size;

    @Schema(description = "Longitud mínima (mm)", example = "230")
    private Integer lengthMinMm;

    @Schema(description = "Longitud máxima (mm)", example = "250")
    private Integer lengthMaxMm;

    @Schema(description = "Peso mínimo (g)", example = "60")
    private Integer weightMinG;

    @Schema(description = "Peso máximo (g)", example = "75")
    private Integer weightMaxG;

    @Schema(description = "Rareza", example = "Común")
    private String rarity;

    @Schema(description = "Colores dominantes", example = "[\"Gris\",\"Blanco\"]")
    private List<String> colors;

    @Schema(description = "URL miniatura (cacheable)", example = "https://cdn.birdex.com/birds/turdus-rufiventris/profile_256.webp")
    private String thumbUrl;

    @Schema(description = "URL imagen media (cacheable)", example = "https://cdn.birdex.com/birds/turdus-rufiventris/profile_600.webp")
    private String imageUrl;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "Spawn zone (name + lat/lon)")
    private List<ZonePointDto> zones;


}
