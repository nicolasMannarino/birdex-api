package com.birdex.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "SaveReportRequest", description = "Payload para registrar un reporte de un avistaje")
public class SaveReportRequest {

    @Schema(description = "Email del usuario que reporta", example = "sofia@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Email
    private String email;

    @Schema(
            description = "ID del avistaje (UUID) a reportar",
            example = "ba8a37c2-0657-4202-a8c8-5ee5576bb691",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "sightingId debe ser un UUID válido")
    private String sightingId;

    @Schema(description = "Descripción del motivo del reporte", example = "Contenido inapropiado", maxLength = 2000)
    @NotBlank
    @Size(max = 2000)
    private String description;
}
