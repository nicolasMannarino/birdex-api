package com.birdex.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(name = "Bird", description = "Detalle de ave")
public class BirdDto {

    @Schema(description = "Scientific name", example = "Turdus rufiventris")
    private String name;

    @Schema(description = "Common name", example = "Zorzal colorado")
    private String commonName;

    @JsonIgnore
    @Schema(description = "(DEPRECATED) Qualitative size", example = "Medium", deprecated = true)
    private String size;

    @Schema(description = "Description", example = "Medium-sized thrush...")
    private String description;

    @Schema(description = "Characteristics", example = "Melodic song; rufous belly...")
    private String characteristics;

    @Schema(
            description = "Image as base64-encoded string.",
            type = "string",
            example = "iVBORw0KGgoAAAANSUhEUgAA... (base64 truncated)"
    )
    private String image;

    @Schema(description = "Rarity (Common, Uncommon, Rare, Epic, Legendary)", example = "Common")
    private String rarity;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "Dominant colors", example = "[\"Brown\",\"Orange\"]")
    private List<String> colors;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(
            description = "Migratory wave: key=month (1..12), value=list of provinces",
            example = "{\"3\":[\"Buenos Aires\"],\"4\":[\"Jujuy\"],\"5\":[\"Río Negro\"]}"
    )
    private Map<Short, List<String>> migratoryWave;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "Measurements", description = "Numeric length and weight ranges (mm/g)")
    private Measurements sizeDetails;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "Zones (name + lat/lon)")
    private List<ZonePointDto> zones;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(name = "Measurements", description = "Length (mm) and weight (g) ranges")
    public static class Measurements {

        @Schema(description = "Minimum length in millimeters", example = "230")
        private Integer lengthMinMm;

        @Schema(description = "Maximum length in millimeters", example = "250")
        private Integer lengthMaxMm;

        @Schema(description = "Minimum weight in grams", example = "60")
        private Integer weightMinG;

        @Schema(description = "Maximum weight in grams", example = "75")
        private Integer weightMaxG;
    }
}