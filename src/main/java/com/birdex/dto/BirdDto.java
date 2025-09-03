package com.birdex.dto;
import lombok.Data;

@Data
public class BirdDto {
    private String name;
    private String commonName;
    private String size;
    private String description;
    private String characteristics;
    private String image;
    private String migratoryWaveUrl;
}
