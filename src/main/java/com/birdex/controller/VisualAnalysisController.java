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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/detect")
@CrossOrigin(origins = "*")
@Tag(name = "Visual Analysis", description = "Detección de aves a partir de una imagen (base64)")
public class VisualAnalysisController {

    @Autowired
    private DetectionService detectionService;

    @PostMapping(
            path = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Detectar ave en imagen (multipart/form-data)",
            description = "Recibe una imagen (campo 'file') y devuelve etiqueta y confianza."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = BirdDetectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BirdDetectResponse> analyzeImage(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(detectionService.detect(file));
    }


    @PostMapping(
            path = "/video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Detectar ave en video (multipart/form-data)",
            description = "Recibe un video corto (campo 'file'). Muestrea frames y devuelve lista y mejor resultado."
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "object",
                            requiredProperties = {"file"}
                    )
            )
    )
    public ResponseEntity<BirdVideoDetectResponse> analyzeVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart(name = "sampleFps", required = false) Integer sampleFps,
            @RequestPart(name = "stopOnFirstAbove", required = false) Boolean stopOnFirstAbove
    ) {
        int fps = (sampleFps == null || sampleFps < 1) ? 1 : sampleFps;
        boolean stop = stopOnFirstAbove != null && stopOnFirstAbove;
        return ResponseEntity.ok(detectionService.detectVideo(file, fps, stop));
    }
}
