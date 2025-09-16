package com.birdex.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "BirdDetectResponse", description = "Resultado de la detección")
public class BirdDetectResponse {

    @Schema(description = "Etiqueta de especie detectada", example = "Turdus rufiventris")
    private String label;

    @Schema(description = "Nivel de confianza (0..1)", example = "0.91")
    private Double trustLevel;

    BirdDetectResponse(String label, Double trustLevel) {
        this.label = label;
        this.trustLevel = trustLevel;
    }

    public BirdDetectResponse() {}

    public static BirdDetectResponseBuilder builder() { return new BirdDetectResponseBuilder(); }

    public static class BirdDetectResponseBuilder {
        private String label;
        private Double trustLevel;
        private String fileBase64;

        public BirdDetectResponseBuilder label(String label) { this.label = label; return this; }
        public BirdDetectResponseBuilder trustLevel(Double trustLevel) { this.trustLevel = trustLevel; return this; }
        public BirdDetectResponseBuilder fileBase64(String fileBase64) { this.fileBase64 = fileBase64; return this; }
        public BirdDetectResponse build() { return new BirdDetectResponse(this.label, this.trustLevel); }
        public String toString() { return "BirdDetectResponse.BirdDetectResponseBuilder(label=" + this.label + ", fileBase64=" + this.fileBase64 + ")"; }
    }
}
