package com.birdex.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BirdDto {
    private String name;
    private String commonName;
    private String size;
    private String description;
    private String characteristics;
    private String image;
    private String migratoryWaveUrl;



}
