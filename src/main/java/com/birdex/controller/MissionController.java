package com.birdex.controller;

import com.birdex.dto.UserMissionDto;
import com.birdex.dto.ErrorResponse;
import com.birdex.service.MissionService;
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
@RequestMapping("/missions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Missions", description = "Gestión de misiones de usuario y reclamo de recompensas")
public class MissionController {

    private final MissionService missionService;

    @GetMapping("/{email}")
    @Operation(
            summary = "Obtener misiones del usuario",
            description = "Devuelve todas las misiones asociadas al usuario indicado por su email."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserMissionDto.class)))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserMissionDto>> getUserMissions(
            @Parameter(description = "Email del usuario", example = "lucas@example.com")
            @PathVariable String email
    ) {
        List<UserMissionDto> missions = missionService.getMissionsByUserEmail(email);
        return ResponseEntity.ok(missions);
    }

    @PostMapping(value = "/claim", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Reclamar recompensa de misión",
            description = "Marca una misión como reclamada (`claimed = true`) y suma los puntos de recompensa al usuario. "
                        + "Controla si el usuario sube de nivel."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recompensa reclamada correctamente"),
            @ApiResponse(responseCode = "400", description = "Request inválido o misión no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error interno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<String> claimMissionReward(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "JSON con el ID de la misión del usuario (`userMissionId`)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Ejemplo de reclamo",
                                            value = """
                                            {
                                              "userMissionId": "9b8f8f4e-6f3c-4f7a-b823-bb19a1d5f1e9"
                                            }
                                            """
                                    )
                            }
                    )
            )
            @RequestBody Map<String, String> request
    ) {
        UUID userMissionId = UUID.fromString(request.get("userMissionId"));
        missionService.claimMissionReward(userMissionId);
        return ResponseEntity.ok("Recompensa reclamada con éxito");
    }
}
