package com.birdex.controller;

import com.birdex.domain.*;
import com.birdex.dto.SightingsForBirdResponse;
import com.birdex.dto.ErrorResponse;
import com.birdex.service.SightingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sighting")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Sightings", description = "Registro y consulta de avistajes")
public class SightingController {

    private final SightingService sightingService;

    @PostMapping
    @Operation(
            summary = "Registrar un avistaje",
            description = "Recibe la imagen base64, datos del ave y ubicación. Devuelve 204 No Content."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registrado"),
            @ApiResponse(responseCode = "400", description = "Request inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> registerSighting(
            @RequestBody(
                    required = true,
                    description = "Datos del avistaje",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SightingRequest.class),
                            examples = @ExampleObject(value = """
                  {
                    "base64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",
                    "email": "user@example.com",
                    "birdName": "Turdus rufiventris",
                    "location": "Parque Saavedra, CABA",
                    "dateTime": "2025-09-06T17:07:45"
                  }
                """)
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody SightingRequest request
    ) {
        sightingService.registerSighting(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{email}/{birdName}")
    @Operation(
            summary = "Imágenes de avistajes por usuario y ave",
            description = "Devuelve imágenes en base64 de los avistajes del usuario para el ave indicada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = SightingImagesByEmailResponse.class))),
            @ApiResponse(responseCode = "404", description = "No hay avistajes",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SightingImagesByEmailResponse> getSightingImagesByUserAndBirdName(
            @Parameter(description = "Email del usuario", example = "user@example.com") @PathVariable String email,
            @Parameter(description = "Nombre científico del ave", example = "Turdus rufiventris") @PathVariable String birdName) {
        SightingImageRequest request = SightingImageRequest.builder()
                .email(email)
                .birdName(birdName)
                .build();
        return ResponseEntity.ok(sightingService.getSightingImagesByUserAndBirdName(request));
    }

    @GetMapping("/{email}/{birdName}/all")
    @Operation(
            summary = "Avistajes propios y de otros para un ave",
            description = "Devuelve secciones separadas: `mine` y `others`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = SightingsForBirdResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sin datos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SightingsForBirdResponse> getSightingsMineAndOthers(
            @Parameter(description = "Email del usuario", example = "user@example.com") @PathVariable String email,
            @Parameter(description = "Nombre científico del ave", example = "Turdus rufiventris") @PathVariable String birdName) {
        return ResponseEntity.ok(sightingService.getSightingsMineAndOthers(email, birdName));
    }

    @GetMapping("/{email}")
    @Operation(summary = "Avistajes de un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = SightingByUserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario sin avistajes",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SightingByUserResponse> getSightingsByUser(
            @Parameter(description = "Email del usuario", example = "user@example.com") @PathVariable String email) {
        return ResponseEntity.ok(sightingService.getSightingsByUser(email));
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar avistajes (paginado)",
            description = "Filtra por rareza, color, zona y tamaño. Paginación por `page` y `sizePage`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<SightingResponse>> searchSightings(
            @Parameter(description = "Rareza (p. ej. Comun, Poco comun, Raro, Epico, Legendario)", example = "Comun")
            @RequestParam(required = false) String rarity,
            @Parameter(description = "Color dominante", example = "Amarillo")
            @RequestParam(required = false) String color,
            @Parameter(description = "Zona/área", example = "AMBA")
            @RequestParam(required = false) String zone,
            @Parameter(description = "Tamaño (Grande, Muy grande, Mediano, Pequeño)", example = "Grande")
            @RequestParam(required = false) String size,
            @Parameter(description = "Número de página (base 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página", example = "20")
            @RequestParam(defaultValue = "20") int sizePage
    ) {
        return ResponseEntity.ok(sightingService.searchSightings(rarity, color, zone, size, page, sizePage));
    }
}
