package com.birdex.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor
@Builder
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

    @Schema(
            description = "Imagen en base64 (cadena codificada).",
            type = "string",
            example = "iVBORw0KGgoAAAANSUhEUgAA... (base64 recortado)"
    )
    private String image;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(
            description = "Ola migratoria: clave=mes (1..12), valor=lista de provincias",
            example = "{\"3\":[\"Buenos Aires\"],\"4\":[\"Jujuy\"],\"5\":[\"Río Negro\"]}"
    )
    private Map<Short, List<String>> migratoryWave;
}