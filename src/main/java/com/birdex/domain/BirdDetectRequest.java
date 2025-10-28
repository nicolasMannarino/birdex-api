package com.birdex.domain;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "BirdDetectRequest", description = "Payload con la imagen a analizar")
public class BirdDetectRequest {
    @Schema(
            description = "Imagen en base64",
            example = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ..."
    )
    private String fileBase64;

    @Schema(description = "Email del usuario", example = "user@example.com")
    private String email;
}
