package com.birdex.entity;


import io.swagger.v3.oas.annotations.media.Schema;
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
}
