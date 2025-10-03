package com.birdex.controller;


import com.birdex.dto.BirdDto;
import com.birdex.dto.BirdListItem;
import com.birdex.dto.BirdProgressResponse;
import com.birdex.entity.BirdSummary;
import com.birdex.service.BirdService;
import com.birdex.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bird")
@CrossOrigin(origins = "*")
@Tag(name = "Birds", description = "Catálogo de aves y búsqueda")
public class BirdController {

    private final BirdService birdService;

    public BirdController(BirdService birdService) {
        this.birdService = birdService;
    }

    @Deprecated
    @GetMapping("/all")
    @Operation(
            summary = "Obtener progreso/listado de aves",
            description = "Devuelve un resumen con perfiles de aves (para pantalla de progreso)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BirdProgressResponse.class),
                            examples = @ExampleObject(value = """
                         { "list": [
                           { "photoBase64": "data:image/jpeg;base64,...",
                             "rarity": "COMMON",
                             "name": "Turdus rufiventris",
                             "commonName": "Zorzal colorado" }
                         ]}
                       """))
            ),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BirdProgressResponse> getBirds() {
        BirdProgressResponse response = birdService.getBirds();
        return ResponseEntity.ok(response);
    }

    @Deprecated
    @GetMapping(value = "/description/{commonName}", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "Descripción por nombre común",
            parameters = @Parameter(name = "commonName",
                    description = "Nombre común del ave",
                    example = "Zorzal colorado")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Texto descriptivo",
                    content = @Content(mediaType = "text/plain",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Ave de tamaño mediano..."))),
            @ApiResponse(responseCode = "404", description = "Ave no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<String> getDescription(@PathVariable String commonName) {
        String response = birdService.getDescription(commonName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{specificName}")
    @Operation(
            summary = "Obtener ave por nombre científico",
            parameters = @Parameter(name = "specificName",
                    description = "Nombre científico (específico)",
                    example = "Turdus rufiventris")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BirdDto.class))),
            @ApiResponse(responseCode = "404", description = "Ave no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BirdDto> getBirdBySpecificName(@PathVariable String specificName) {
        BirdDto response = birdService.getBySpecificName(specificName);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Buscar aves (paginado)",
            description = """
        Filtra por rareza, color, tamaño y por rangos de longitud/peso.
        Devuelve URLs cacheables de imagen (miniatura y media).
        Parámetros numéricos opcionales; orden configurable con `sort=campo,asc|desc`.
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultados paginados (Page<BirdListItem>)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BirdListItem.class),
                            examples = @ExampleObject(value = """
                {
                  "content": [{
                    "birdId": "6c8a0a7d-1cfc-4d13-9e4c-91b2d3f4b9d1",
                    "name": "Turdus rufiventris",
                    "commonName": "Zorzal colorado",
                    "size": "Grande",
                    "lengthMinMm": 230,
                    "lengthMaxMm": 250,
                    "weightMinG": 60,
                    "weightMaxG": 75,
                    "rarity": "Común",
                    "colors": ["Gris","Blanco"],
                    "thumbUrl": "https://cdn.birdex.com/birds/turdus-rufiventris/profile_256.webp",
                    "imageUrl": "https://cdn.birdex.com/birds/turdus-rufiventris/profile_600.webp"
                  }],
                  "pageable": { "...": "..." },
                  "totalElements": 1,
                  "totalPages": 1,
                  "last": true,
                  "size": 20,
                  "number": 0,
                  "sort": { "...": "..." },
                  "first": true,
                  "numberOfElements": 1,
                  "empty": false
                }
                """))
            ),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Page<BirdListItem> searchBirds(
            @Parameter(description = "Rareza (p. ej. Común, Poco común, Raro, Épico, Legendario)", example = "Común")
            @RequestParam(required = false) String rarity,
            @Parameter(description = "Color dominante", example = "amarillo")
            @RequestParam(required = false) String color,
            @Schema(description = "Tamaño (Grande, Muy grande, Mediano, Pequeño)", example = "Grande")
            @RequestParam(required = false) String size,

            @Parameter(description = "Longitud mínima (mm)", example = "180")
            @RequestParam(required = false) Integer lengthMinMm,
            @Parameter(description = "Longitud máxima (mm)", example = "200")
            @RequestParam(required = false) Integer lengthMaxMm,
            @Parameter(description = "Peso mínimo (g)", example = "40")
            @RequestParam(required = false) Integer weightMinG,
            @Parameter(description = "Peso máximo (g)", example = "65")
            @RequestParam(required = false) Integer weightMaxG,

            @Parameter(description = "Número de página (base 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página", example = "20")
            @RequestParam(defaultValue = "20") int sizePage,
            @Parameter(description = "Orden: `campo,asc|desc`", example = "name,asc")
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, sizePage, sortObj);

        Integer lenMin = cleanPositive(lengthMinMm);
        Integer lenMax = cleanPositive(lengthMaxMm);
        if (lenMin != null && lenMax != null && lenMin > lenMax) {
            int tmp = lenMin; lenMin = lenMax; lenMax = tmp;
        }

        Integer wMin = cleanPositive(weightMinG);
        Integer wMax = cleanPositive(weightMaxG);
        if (wMin != null && wMax != null && wMin > wMax) {
            int tmp = wMin; wMin = wMax; wMax = tmp;
        }

        return birdService.search(rarity, color, size, lenMin, lenMax, wMin, wMax, pageable);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.ASC, "name");
        String[] parts = sort.split(",", 2);
        String field = parts[0].isBlank() ? "name" : parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }

    private Integer cleanPositive(Integer v) {
        return (v != null && v > 0) ? v : null;
    }
}