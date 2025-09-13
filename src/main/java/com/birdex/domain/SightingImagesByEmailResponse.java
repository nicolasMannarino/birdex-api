package com.birdex.domain;


import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class SightingImagesByEmailResponse {
    private List<String> base64Images = new ArrayList<>();
}
