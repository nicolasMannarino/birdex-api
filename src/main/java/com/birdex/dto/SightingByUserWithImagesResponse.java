package com.birdex.dto;


import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data @Builder
@Schema(name = "SightingByUserWithImagesResponse", description = "Avistajes de un usuario con imÃ¡genes")
public class SightingByUserWithImagesResponse {
    @ArraySchema(arraySchema = @Schema(description = "Listado del usuario"))
    private List<SightingWithImagesResponse> sightingResponseList;
}