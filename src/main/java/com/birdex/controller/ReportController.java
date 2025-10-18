package com.birdex.controller;

import com.birdex.domain.SaveReportRequest;
import com.birdex.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/reports", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Registro y consulta de reportes")
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "Crear un nuevo reporte",
            description = "Registra un reporte para un avistaje identificado por su sightingId. "
                    + "Requiere que el email pertenezca a un usuario existente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reporte creado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "string", format = "uuid"),
                            examples = @ExampleObject(name = "UUID", value = "\"37ad1d92-06c4-4d5c-8988-ffd6b835e3aa\"")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Payload inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario o avistaje no encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe reporte para ese avistaje (o del mismo usuario)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveReport(
            @RequestBody(
                    description = "Datos para registrar el reporte",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SaveReportRequest.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo",
                                    value = """
                                {
                                  "email": "sofia@example.com",
                                  "sightingId": "ba8a37c2-0657-4202-a8c8-5ee5576bb691",
                                  "description": "Contenido inapropiado"
                                }
                                """
                            )
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody SaveReportRequest saveReportRequest) {

        return ResponseEntity.ok(reportService.reportSave(saveReportRequest).toString());
    }
}
