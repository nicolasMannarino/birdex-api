package com.birdex.controller;

import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import com.birdex.domain.BirdVideoDetectRequest;
import com.birdex.domain.BirdVideoDetectResponse;
import com.birdex.service.DetectionService;
import com.birdex.dto.ErrorResponse; // si tenés tu DTO de error global
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/detect")
@CrossOrigin(origins = "*")
@Tag(name = "Visual Analysis", description = "Detección de aves a partir de una imagen (base64)")
public class VisualAnalysisController {

    @Autowired
    private DetectionService detectionService;

    @PostMapping(consumes = "application/json", produces = "application/json")
    @Operation(
            summary = "Detectar ave en imagen",
            description = "Recibe una imagen en base64 y devuelve la etiqueta de especie detectada y el nivel de confianza."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detección exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BirdDetectResponse.class),
                            examples = @ExampleObject(value = """
                  { "label": "Turdus rufiventris", "trustLevel": 0.91 }
                """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Request inválido",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity< BirdDetectResponse > analyze(
            @RequestBody(
                    required = true,
                    description = "Imagen a analizar",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BirdDetectRequest.class),
                            examples = @ExampleObject(value = """
                  { "fileBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ..." }
                """)
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody BirdDetectRequest request
    ) {
        BirdDetectResponse response = detectionService.detect(request);
        return ResponseEntity.ok(response);
    }


    @PostMapping(value = "/video", consumes = "application/json", produces = "application/json")
    @Operation(summary = "Detectar ave en video",
            description = "Recibe un video en base64 (hasta 15s). Muestrea frames y devuelve lista y mejor resultado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = BirdVideoDetectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public ResponseEntity<BirdVideoDetectResponse> analyzeVideo(
            @org.springframework.web.bind.annotation.RequestBody BirdVideoDetectRequest request
    ) {
        if (request == null || request.getFileBase64() == null || request.getFileBase64().isBlank()) {
            throw new IllegalArgumentException("Body inválido: falta fileBase64");
        }
        return ResponseEntity.ok(detectionService.detectVideo(request));
    }
}
