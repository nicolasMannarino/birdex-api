package com.birdex.domain;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BirdVideoDetectRequest {
    @JsonProperty("fileBase64")
    @JsonAlias({"videoBase64","base64","file_base64"})
    private String fileBase64;
    private Integer sampleFps = 1;
    private Boolean stopOnFirstAbove = false;
}
