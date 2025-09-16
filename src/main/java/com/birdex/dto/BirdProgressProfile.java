package com.birdex.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data @Builder
@Schema(name = "BirdProgressProfile", description = "Perfil resumido de ave para progreso")
public class BirdProgressProfile {
    @Schema(description = "Imagen en base64 (opcional)", example = "data:image/jpeg;base64,...")
    private String photoBase64;

    @Schema(description = "Rareza", example = "COMMON")
    private String rarity;

    @Schema(description = "Nombre científico", example = "Turdus rufiventris")
    private String name;

    @Schema(description = "Nombre común", example = "Zorzal colorado")
    private String commonName;
}
