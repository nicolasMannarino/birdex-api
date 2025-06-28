package com.birdex.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/detect")
public class MockController {


    @PostMapping
    public ResponseEntity<Map<String, Object>> mockResponse(@RequestBody Map<String, String> request) {
        String file = request.get("file");
        Map<String, Object> response = new HashMap<>();

        String mimeType = null;
        if (file != null && file.startsWith("data:")) {
            int semicolonIndex = file.indexOf(';');
            if (semicolonIndex > 5) {
                mimeType = file.substring(5, semicolonIndex);
            }
        }

        if (mimeType != null && mimeType.startsWith("image/")) {
            response.put("etiqueta", "Paroaria coronata");
            response.put("confianza", 0.9823);
            response.put("imagen", file);
        } else if (mimeType != null && mimeType.startsWith("video/")) {
            response.put("etiqueta", "Ave no identificada");
            response.put("confianza", 0.0);
            response.put("imagen", file);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato no soportado"));
        }

        return ResponseEntity.ok(response);
    }
}
