package com.birdex.domain;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data @Builder
@Schema(name = "SightingImagesByEmailResponse", description = "URLs públicas de imágenes de avistajes")
public class SightingImagesByEmailResponse {
    @ArraySchema(arraySchema = @Schema(description = "Listado de imágenes (thumb + image)"))
    private List<SightingImageItem> images = new ArrayList<>();
}