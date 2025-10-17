package com.birdex.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/")
@Tag(name = "Tiles", description = "Descarga del archivo de tiles de Argentina")
public class FileController {

    private static final String FILE_PATH = "tiles/tiles_argentina.zip";

    @GetMapping(value = "tiles", produces = "application/zip")
    @Operation(
            summary = "Descargar archivo de tiles de Argentina",
            description = "Devuelve el archivo `tiles_argentina.zip` ubicado en la carpeta `/tiles` del proyecto."
    )
    @ApiResponse(responseCode = "200", description = "Archivo encontrado y enviado correctamente")
    @ApiResponse(responseCode = "404", description = "Archivo no encontrado")
    public ResponseEntity<FileSystemResource> getTilesFile() {
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tiles_argentina.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }
}
