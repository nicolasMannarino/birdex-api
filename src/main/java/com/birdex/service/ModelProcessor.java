package com.birdex.service;

import com.birdex.domain.BirdDetectResponse;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class ModelProcessor {

    public BirdDetectResponse evaluate(String fileBase64) throws IOException, InterruptedException {
        if (fileBase64 == null || fileBase64.isEmpty()) {
            throw new IllegalArgumentException("El string base64 está vacío o es nulo");
        }

        ProcessBuilder pb = new ProcessBuilder("python", "src/main/resources/test_predict_stdin.py");
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(fileBase64);
            writer.flush();
        }

        process.getOutputStream().close();

        String resultLine = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                resultLine = line.trim();
                System.out.println("[Python] " + resultLine);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error ejecutando el script de Python, código: " + exitCode);
        }

        if (resultLine == null || !resultLine.contains(",")) {
            throw new RuntimeException("Formato de salida inesperado del script: " + resultLine);
        }

        String[] parts = resultLine.split(",", 2);
        String label = parts[0].trim();
        double trustLevel = Double.parseDouble(parts[1].trim());

        return BirdDetectResponse.builder()
                .label(label)
                .trustLevel(trustLevel)
                .build();
    }
}