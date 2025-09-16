package com.birdex.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
@Schema(name = "Bird", description = "Detalle de ave")
public class BirdDto {
    @Schema(description = "Nombre científico", example = "Turdus rufiventris")
    private String name;

    @Schema(description = "Nombre común", example = "Zorzal colorado")
    private String commonName;

    @Schema(description = "Tamaño (Grande, Muy grande, Mediano, Pequeño)", example = "Grande")
    private String size;

    @Schema(description = "Descripción", example = "Ave de tamaño mediano presente en...")
    private String description;

    @Schema(description = "Características", example = "Canto melodioso; pecho rojizo...")
    private String characteristics;

    @Schema(description = "URL de imagen", example = "https://cdn.birdex.com/img/zorzal.jpg")
    private String image;

    @Schema(description = "URL a ola migratoria (si aplica)", example = "https://...")
    private String migratoryWaveUrl;
}
