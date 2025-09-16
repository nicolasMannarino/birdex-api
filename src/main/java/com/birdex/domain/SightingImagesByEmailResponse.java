package com.birdex.domain;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data @Builder
@Schema(name = "SightingImagesByEmailResponse", description = "Imágenes base64 de un usuario para un ave")
public class SightingImagesByEmailResponse {
    @ArraySchema(arraySchema = @Schema(description = "Imágenes en base64"))
    private List<String> base64Images = new ArrayList<>();
}

