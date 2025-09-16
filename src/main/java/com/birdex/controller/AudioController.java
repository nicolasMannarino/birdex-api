package com.birdex.controller;

import com.birdex.domain.BirdnetAnalyzeRequest;
import com.birdex.domain.Detection;
import com.birdex.service.BirdnetService;
import com.birdex.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audio")
@Tag(name = "Audio", description = "Análisis de audio con BirdNET")
public class AudioController {

    private final BirdnetService birdnetService;

    public AudioController(BirdnetService birdnetService) {
        this.birdnetService = birdnetService;
    }

    @PostMapping(value = "/analyze", consumes = "application/json", produces = "application/json")
    @Operation(
            summary = "Analizar un audio",
            description = "Procesa el audio (base64) y devuelve la detección más probable."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Análisis exitoso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Detection.class),
                            examples = @ExampleObject(name = "OK", value = """
                  {
                    "start_tim": 0.0,
                    "end_time": 10.0,
                    "label": "Turdus rufiventris",
                    "confidence": 0.93
                  }
                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request inválido (validación o JSON malformado)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "VALIDATION_ERROR", value = """
                  {
                    "timestamp": "2025-09-16T18:45:12Z",
                    "status": 400,
                    "error": "Bad Request",
                    "code": "VALIDATION_ERROR",
                    "message": "Validation failed",
                    "path": "/audio/analyze",
                    "method": "POST",
                    "violations": [
                      {"field": "audio_base64", "reason": "audio_base64 is required and cannot be empty", "rejectedValue": ""}
                    ]
                  }
                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Detection> analyze(
            @RequestBody(
                    required = true,
                    description = "Datos necesarios para ejecutar el análisis",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BirdnetAnalyzeRequest.class),
                            examples = @ExampleObject(name = "Ejemplo", value = """
                  {
                    "audio_base64": "UklGRiQAAABXQVZFZm10...",
                    "min_conf": 0.8
                  }
                """)
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody BirdnetAnalyzeRequest request
    ) {
        Detection resp = birdnetService.analyze(request);
        return ResponseEntity.ok(resp);
    }
}
