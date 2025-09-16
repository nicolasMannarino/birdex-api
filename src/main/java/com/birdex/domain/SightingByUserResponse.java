package com.birdex.domain;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
@Schema(name = "SightingByUserResponse", description = "Avistajes de un usuario")
public class SightingByUserResponse {
    @ArraySchema(arraySchema = @Schema(description = "Listado del usuario"))
    private List<SightingResponse> sightingResponseList;
}
