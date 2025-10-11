package com.birdex.controller;

import com.birdex.dto.UserAchievementDto;
import com.birdex.dto.ErrorResponse;
import com.birdex.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/achievements")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Achievements", description = "Gestión y reclamo de logros de usuario")
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping("/{email}")
    @Operation(
            summary = "Obtener logros del usuario",
            description = "Devuelve todos los logros asociados a un usuario por su email, incluyendo progreso, fecha de obtención y estado de reclamo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserAchievementDto.class)))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserAchievementDto>> getAchievementsByUser(
            @Parameter(description = "Email del usuario", example = "lucas@example.com")
            @PathVariable String email
    ) {
        return ResponseEntity.ok(achievementService.getAchievementsByUserEmail(email));
    }

    @PostMapping(value = "/claim", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Reclamar logro obtenido",
            description = "Marca un logro del usuario como reclamado (`claimed = true`) si ya fue obtenido (`obtainedAt` no nulo)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logro reclamado correctamente"),
            @ApiResponse(responseCode = "400", description = "Request inválido o logro no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<String> claimAchievement(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "JSON con el ID del logro del usuario (`userAchievementId`)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Ejemplo de reclamo",
                                            value = """
                                            {
                                              "userAchievementId": "2f4d1b3e-8f4c-4ac7-a2a8-8a49a6d99d3b"
                                            }
                                            """
                                    )
                            }
                    )
            )
            @RequestBody Map<String, String> payload
    ) {
        UUID userAchievementId = UUID.fromString(payload.get("userAchievementId"));
        achievementService.claimAchievement(userAchievementId);
        return ResponseEntity.ok("Logro reclamado correctamente");
    }
}


