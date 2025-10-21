package com.birdex.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SightingImageItem", description = "URLs públicas de una imagen de avistaje")
public class SightingImageItem {
    @Schema(description = "Thumbnail ~256px", example = "http://localhost:9100/birds/sightings/user@example.com/turdus-rufiventris/foo_256.jpg")
    private String thumbUrl;

    @Schema(description = "Imagen ~600px", example = "http://localhost:9100/birds/sightings/user@example.com/turdus-rufiventris/foo_600.jpg")
    private String imageUrl;
}