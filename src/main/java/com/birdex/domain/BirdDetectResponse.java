package com.birdex.domain;

import lombok.Data;


@Data
public class BirdDetectResponse {
    private String label;
    private Double trustLevel;
    private String fileBase64;

    BirdDetectResponse(String label, Double trustLevel, String fileBase64) {
        this.label = label;
        this.trustLevel = trustLevel;
        this.fileBase64 = fileBase64;
    }

    public static BirdDetectResponseBuilder builder() {
        return new BirdDetectResponseBuilder();
    }

    public static class BirdDetectResponseBuilder {
        private String label;
        private Double trustLevel;
        private String fileBase64;

        public BirdDetectResponseBuilder() {
        }

        public BirdDetectResponseBuilder label(String label) {
            this.label = label;
            return this;
        }

        public BirdDetectResponseBuilder trustLevel(Double trustLevel) {
            this.trustLevel = trustLevel;
            return this;
        }

        public BirdDetectResponseBuilder fileBase64(String fileBase64) {
            this.fileBase64 = fileBase64;
            return this;
        }

        public BirdDetectResponse build() {
            return new BirdDetectResponse(this.label, this.trustLevel, this.fileBase64);
        }

        public String toString() {
            return "BirdDetectResponse.BirdDetectResponseBuilder(label=" + this.label + ", trustLevel=" + this.trustLevel + ", fileBase64=" + this.fileBase64 + ")";
        }
    }
}
