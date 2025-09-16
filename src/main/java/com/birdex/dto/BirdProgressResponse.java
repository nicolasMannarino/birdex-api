package com.birdex.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
@Schema(name = "BirdProgressResponse", description = "Listado de perfiles de progreso de aves")
public class BirdProgressResponse {
    @ArraySchema(arraySchema = @Schema(description = "Perfiles"), schema = @Schema(implementation = BirdProgressProfile.class))
    private List<BirdProgressProfile> list;
}