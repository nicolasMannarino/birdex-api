package com.birdex.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(name = "BirdSummary", description = "Resumen de ave para búsquedas")
public interface BirdSummary {

    @Schema(description = "ID", example = "6c8a0a7d-1cfc-4d13-9e4c-91b2d3f4b9d1")
    UUID getBirdId();

    @Schema(description = "Nombre científico", example = "Turdus rufiventris")
    String getName();

    @Schema(description = "Nombre común", example = "Zorzal colorado")
    String getCommonName();

    @Schema(description = "URL de imagen", example = "https://cdn.birdex.com/img/zorzal.jpg")
    String getImage();

    @Schema(description = "Tamaño (Grande, Muy grande, Mediano, Pequeño)", example = "Grande")
    String getSize();

    @Schema(description = "Longitud mínima (mm)", example = "230")
    Integer getLengthMinMm();

    @Schema(description = "Longitud máxima (mm)", example = "250")
    Integer getLengthMaxMm();

    @Schema(description = "Peso mínimo (g)", example = "60")
    Integer getWeightMinG();

    @Schema(description = "Peso máximo (g)", example = "75")
    Integer getWeightMaxG();

    @Schema(description = "Rareza", example = "Común")
    String getRarity();

    @Schema(description = "Colores dominantes", example = "[\"Gris\",\"Blanco\"]")
    List<String> getColors();

    @Schema(description = "Zonas de aparición")
    List<ZonePoint> getZones();

    @Schema(name = "ZonePoint", description = "Zona de aparición con coordenadas")
    interface ZonePoint {
        @Schema(description = "Nombre de la zona", example = "Buenos Aires")
        String name();

        @Schema(description = "Latitud", example = "-34.921450")
        BigDecimal latitude();

        @Schema(description = "Longitud", example = "-57.954530")
        BigDecimal longitude();
    }
}
