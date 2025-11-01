package com.birdex.domain;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "Detection", description = "Resultado de detección BirdNET")
public class Detection {
    @Schema(description = "Inicio de ventana analizada (segundos)", example = "0.0")
    private double start_tim;
    @Schema(description = "Fin de ventana analizada (segundos)", example = "10.0")
    private double end_time;
    @Schema(description = "Etiqueta de especie predicha", example = "Turdus rufiventris")
    private String label;
    @Schema(description = "Confianza de la predicción (0..1)", example = "0.93")
    private double confidence;
    @Schema(description = "ID del avistamiento asociado en la base de datos", example = "08d6997d-7685-4993-b5f0-48f2142c28c9")
    private UUID sightingId;
}