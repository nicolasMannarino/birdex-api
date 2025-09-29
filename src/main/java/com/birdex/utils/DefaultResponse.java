package com.birdex.utils;

import com.birdex.domain.BirdnetAnalyzeResponse;
import com.birdex.domain.Detection;

import java.util.List;

public class DefaultResponse {

    public static Detection defaultDetectionResponse(){
        Detection d = new Detection();
        d.setStart_tim(0.0);
        d.setEnd_time(0.0);
        d.setLabel("Desconocida");
        d.setConfidence(0.99);

        return d;
    }
    public static BirdnetAnalyzeResponse defaultResponse() {
        Detection d = defaultDetectionResponse();
        BirdnetAnalyzeResponse r = new BirdnetAnalyzeResponse();
        r.setDetections(List.of(d));
        return r;
    }
}
