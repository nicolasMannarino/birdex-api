package com.birdex.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(name = "ZonePoint", description = "Zona de aparición con coordenadas")
public class ZonePointDto {
    @Schema(description = "Nombre de la zona", example = "Buenos Aires")
    private String name;

    @Schema(description = "Latitud", example = "-34.921450")
    private BigDecimal latitude;

    @Schema(description = "Longitud", example = "-57.954530")
    private BigDecimal longitude;
}