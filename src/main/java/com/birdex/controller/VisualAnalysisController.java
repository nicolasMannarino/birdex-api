package com.birdex.controller;


import com.birdex.domain.BirdDetectRequest;
import com.birdex.domain.BirdDetectResponse;
import com.birdex.domain.BirdVideoDetectRequest;
import com.birdex.domain.BirdVideoDetectResponse;
import com.birdex.dto.ErrorResponse;
import com.birdex.service.DetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/detect")
@CrossOrigin(origins = "*")
@Tag(name = "Visual Analysis", description = "Detección de aves a partir de base64")
@RequiredArgsConstructor
public class VisualAnalysisController {

    private final DetectionService detectionService;

    @PostMapping(
            path = "/image",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Detectar ave en imagen (JSON base64)",
            description = "Recibe JSON con 'fileBase64' y devuelve etiqueta y confianza."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = BirdDetectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BirdDetectResponse> analyzeImage(@Valid @RequestBody BirdDetectRequest request) {
        return ResponseEntity.ok(detectionService.detect(request));
    }

    @PostMapping(
            path = "/video",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Detectar ave en video (JSON base64)",
            description = "Recibe JSON con 'fileBase64'. Muestrea frames y devuelve mejor resultado."
    )
    public ResponseEntity<BirdVideoDetectResponse> analyzeVideo(@Valid @RequestBody BirdVideoDetectRequest request) {
        return ResponseEntity.ok(detectionService.detectVideo(request));
    }

    @PostMapping(
            path = "/video-multipart",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Detectar ave en video (multipart/form-data)",
            description = "Subís el video como archivo y pasás email + params opcionales en el form."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = BirdVideoDetectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BirdVideoDetectResponse> analyzeVideoMultipart(
            @RequestPart("file") MultipartFile file,
            @RequestPart("email") String email,
            @RequestPart(name = "sampleFps", required = false) Integer sampleFps,
            @RequestPart(name = "stopOnFirstAbove", required = false) Boolean stopOnFirstAbove
    ) {
        return ResponseEntity.ok(
                detectionService.detectVideoMultipart(file, email, sampleFps, stopOnFirstAbove)
        );
    }
}