package com.birdex.controller;

import com.birdex.config.BucketProperties;
import com.birdex.service.BucketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin panel", description = "Panel de administracion")
public class AdminController {

    private final BucketService bucketService;
    private final BucketProperties props;

    @PostMapping(value = "/{birdName}/profile", consumes = "application/json", produces = "application/json")
    @Operation(
            summary = "Subir imagen de perfil (base64)",
            description = "Guarda la imagen de perfil de un ave en el bucket y devuelve bucket, key y URL pública."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subida exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = """
                  { "bucket": "birds", "key": "profiles/zorzal.jpg", "url": "https://cdn.example.com/birds/profiles/zorzal.jpg" }
                """))),
            @ApiResponse(responseCode = "400", description = "Request inválido (base64 o contentType faltantes)"),
            @ApiResponse(responseCode = "500", description = "Error interno al subir al bucket")
    })
    public ResponseEntity<Map<String, String>> uploadProfileBase64(
            @Parameter(description = "Nombre del ave (slug o común)", example = "zorzal-colorado")
            @PathVariable String birdName,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Imagen codificada en base64 y su content-type",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BirdProfileBase64Request.class),
                            examples = @ExampleObject(name = "Ejemplo", value = """
                      {
                        "base64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",
                        "contentType": "image/jpeg"
                      }
                    """)
                    )
            )
            @RequestBody BirdProfileBase64Request body
    ) {
        String key = bucketService.putBirdProfileBase64(
                birdName,
                body.getBase64(),
                body.getContentType()
        );

        String bucket = props.getBirds().getBucket();
        String url = bucketService.buildPublicUrl(bucket, key);

        return ResponseEntity.ok(Map.of(
                "bucket", bucket,
                "key", key,
                "url", url
        ));
    }

    @GetMapping(value = "/{birdName}/profile/base64", produces = "application/json")
    @Operation(
            summary = "Obtener imagen en base64 (sin data URI)",
            description = "Devuelve el contenido base64 de la imagen de perfil."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = """
                  { "base64": "/9j/4AAQSkZJRgABAQ..." }
                """))),
            @ApiResponse(responseCode = "404", description = "No existe perfil para ese ave")
    })
    public ResponseEntity<Map<String, String>> getProfileBase64(
            @Parameter(description = "Nombre del ave", example = "zorzal-colorado")
            @PathVariable String birdName
    ) {
        String base64 = bucketService.getBirdProfileBase64(birdName);
        return ResponseEntity.ok(Map.of("base64", base64));
    }

    @GetMapping(value = "/{birdName}/profile/data-uri", produces = "application/json")
    @Operation(
            summary = "Obtener imagen como data URI",
            description = "Devuelve la imagen lista para `<img src=\"...\">`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = """
                  { "dataUri": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ..." }
                """))),
            @ApiResponse(responseCode = "404", description = "No existe perfil para ese ave")
    })
    public ResponseEntity<Map<String, String>> getProfileDataUri(
            @Parameter(description = "Nombre del ave", example = "zorzal-colorado")
            @PathVariable String birdName
    ) {
        String dataUri = bucketService.getBirdProfileDataUri(birdName);
        return ResponseEntity.ok(Map.of("dataUri", dataUri));
    }

    @PostMapping(value = "/profiles/bulk", consumes = "application/json", produces = "application/json")
    @Operation(
            summary = "Subir múltiples imágenes de perfil (base64)",
            description = "Carga en lote imágenes de perfil de aves en el bucket. Cada ítem incluye birdName, base64 y contentType. Responde por ítem con bucket, key, url o error."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proceso completado (con o sin errores por ítem)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BulkUploadResult[].class),
                            examples = @ExampleObject(value = """
                        [
                          { "birdName": "zorzal-colorado", "bucket": "birds", "key": "profiles/zorzal.jpg", "url": "https://cdn.example.com/birds/profiles/zorzal.jpg" },
                          { "birdName": "tucan-toco", "error": "Content-Type no soportado: image/gif" }
                        ]
                        """))),
            @ApiResponse(responseCode = "400", description = "Request inválido (lista vacía o datos faltantes)"),
            @ApiResponse(responseCode = "500", description = "Error inesperado")
    })
    public ResponseEntity<List<BulkUploadResult>> uploadProfilesBulk(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Lista de perfiles a subir",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BulkBirdProfileBase64Request.class),
                            examples = @ExampleObject(name = "Ejemplo", value = """
                        {
                          "uploads": [
                            {
                              "birdName": "zorzal-colorado",
                              "base64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",
                              "contentType": "image/jpeg"
                            },
                            {
                              "birdName": "tucan-toco",
                              "base64": "/9j/4AAQSkZJRgABAQ...", 
                              "contentType": "image/png"
                            }
                          ]
                        }
                        """)
                    )
            )
            @RequestBody BulkBirdProfileBase64Request body
    ) {
        if (body == null || body.getUploads() == null || body.getUploads().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String bucket = props.getBirds().getBucket();
        List<BulkUploadResult> results = new java.util.ArrayList<>();

        for (BulkItem item : body.getUploads()) {
            BulkUploadResult.BulkUploadResultBuilder res = BulkUploadResult.builder()
                    .birdName(item.getBirdName());

            try {
                if (item.getBirdName() == null || item.getBirdName().isBlank()
                        || item.getBase64() == null || item.getBase64().isBlank()
                        || item.getContentType() == null || item.getContentType().isBlank()) {
                    results.add(res.error("Faltan campos requeridos: birdName/base64/contentType").build());
                    continue;
                }

                String key = bucketService.putBirdProfileBase64(
                        item.getBirdName(),
                        item.getBase64(),
                        item.getContentType()
                );

                String url = bucketService.buildPublicUrl(bucket, key);

                results.add(res
                        .bucket(bucket)
                        .key(key)
                        .url(url)
                        .build());
            } catch (Exception e) {
                results.add(res.error(e.getMessage()).build());
            }
        }

        return ResponseEntity.ok(results);
    }

    @Data
    @Schema(name = "BulkBirdProfileBase64Request", description = "Payload para subir varias imágenes de perfil en base64")
    public static class BulkBirdProfileBase64Request {
        @Schema(description = "Lista de cargas a procesar")
        private java.util.List<BulkItem> uploads;
    }

    @Data
    @Schema(name = "BulkItem", description = "Ítem individual de carga de imagen de perfil")
    public static class BulkItem {
        @Schema(description = "Nombre del ave (slug o común)", example = "zorzal-colorado")
        private String birdName;

        @Schema(description = "Imagen en base64 (con o sin prefijo data URI)",
                example = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...")
        private String base64;

        @Schema(description = "Content-Type de la imagen", example = "image/jpeg")
        private String contentType;
    }

    @lombok.Data
    @lombok.Builder
    @Schema(name = "BulkUploadResult", description = "Resultado por ítem del proceso de carga")
    public static class BulkUploadResult {
        @Schema(description = "Nombre del ave", example = "zorzal-colorado")
        private String birdName;

        @Schema(description = "Bucket destino", example = "birds")
        private String bucket;

        @Schema(description = "Clave/Key generada", example = "profiles/zorzal.jpg")
        private String key;

        @Schema(description = "URL pública resultante", example = "https://cdn.example.com/birds/profiles/zorzal.jpg")
        private String url;

        @Schema(description = "Mensaje de error si el ítem falló")
        private String error;
    }

    @Data
    @Schema(name = "BirdProfileBase64Request", description = "Payload para subir imagen de perfil en base64")
    public static class BirdProfileBase64Request {
        @Schema(description = "Imagen en base64 (con o sin prefijo data URI)",
                example = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...")
        private String base64;

        @Schema(description = "Content-Type de la imagen", example = "image/jpeg")
        private String contentType;
    }
}
