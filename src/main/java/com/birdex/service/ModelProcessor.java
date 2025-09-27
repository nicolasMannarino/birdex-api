package com.birdex.service;

import com.birdex.domain.BirdDetectResponse;
import com.birdex.domain.BirdVideoDetectResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.*;


import lombok.SneakyThrows;
import java.nio.charset.StandardCharsets;


@Component
public class ModelProcessor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SneakyThrows
    public BirdDetectResponse evaluateImage(String fileBase64) {
        if (fileBase64 == null || fileBase64.isEmpty()) {
            throw new IllegalArgumentException("El string base64 está vacío o es nulo");
        }

        ProcessBuilder pb = new ProcessBuilder("python", "src/main/resources/test_predict_stdin.py");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(fileBase64);
            writer.flush();
        }
        process.getOutputStream().close();

        String lastLine = null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastLine = line.trim();
                System.out.println("[Python IMG] " + lastLine);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error ejecutando script imagen, code: " + exitCode);
        }

        if (lastLine == null || !lastLine.contains(",")) {
            throw new RuntimeException("Salida inesperada script imagen: " + lastLine);
        }
        String[] parts = lastLine.split(",", 2);
        return BirdDetectResponse.builder()
                .label(parts[0].trim())
                .trustLevel(Double.parseDouble(parts[1].trim()))
                .build();
    }

    @SneakyThrows
    public BirdVideoDetectResponse evaluateVideo(String fileBase64, int sampleFps, boolean stopOnFirstAbove) {
        if (fileBase64 == null || fileBase64.isEmpty()) {
            throw new IllegalArgumentException("El string base64 está vacío o es nulo");
        }

        ProcessBuilder pb = new ProcessBuilder("python", "src/main/resources/test_predict_video_stdin.py");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        var payload = MAPPER.createObjectNode();
        payload.put("fileBase64", fileBase64);
        payload.put("sampleFps", sampleFps);
        payload.put("stopOnFirstAbove", stopOnFirstAbove);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(MAPPER.writeValueAsString(payload));
            writer.flush();
        }
        process.getOutputStream().close();

        String jsonLine = null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonLine = line.trim();
                System.out.println("[Python VID] " + jsonLine);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error ejecutando script video, code: " + exitCode);
        }
        if (jsonLine == null || jsonLine.isEmpty()) {
            throw new RuntimeException("Salida vacía del script de video");
        }

        return MAPPER.readValue(jsonLine, BirdVideoDetectResponse.class);
    }
}