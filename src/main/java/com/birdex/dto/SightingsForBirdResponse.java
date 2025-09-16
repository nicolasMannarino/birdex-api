package com.birdex.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
@Schema(name = "SightingsForBirdResponse", description = "Avistajes propios y de otros para un ave")
public class SightingsForBirdResponse {

    @Schema(description = "Nombre científico", example = "Turdus rufiventris")
    private String birdName;

    @Schema(description = "Nombre común", example = "Zorzal colorado")
    private String commonName;

    @Schema(description = "Rareza (p. ej. Comun, Poco comun, Raro, Epico, Legendario)", example = "Comun")
    private String rarity;

    @Schema(description = "Foto de perfil (base64)", example = "data:image/jpeg;base64,/9j/4AAQ...")
    private String profilePhotoBase64;

    @ArraySchema(arraySchema = @Schema(description = "Mis avistajes"))
    private List<SightingFullResponse> mine;

    @ArraySchema(arraySchema = @Schema(description = "Avistajes de otros usuarios"))
    private List<SightingFullResponse> others;
}
