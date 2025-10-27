package com.birdex.service;

import com.birdex.domain.BirdDetectResponse;
import com.birdex.domain.BirdVideoDetectResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class ModelProcessor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String IMG_WORKER = "src/main/resources/birdex_worker_image.py";
    private static final String VID_WORKER = "src/main/resources/birdex_worker_video.py";

    private Process imgProc;
    private BufferedReader imgReader;            // stdout JSON
    private BufferedOutputStream imgWriter;      // stdin bytes

    private Process vidProc;
    private BufferedReader vidReader;
    private BufferedOutputStream vidWriter;
    private int lastVideoFps = 1;
    private boolean lastVideoStop = false;

    /* ================= API ================= */

    @SneakyThrows
    public BirdDetectResponse evaluateImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Imagen vacía o nula");
        }
        ensureImgWorker();
        String line = askWorker(imgWriter, imgReader, imageBytes, 10_000);
        return parseImageResponse(line);
    }

    @SneakyThrows
    public BirdVideoDetectResponse evaluateVideo(byte[] videoBytes, int sampleFps, boolean stopOnFirstAbove) {
        if (videoBytes == null || videoBytes.length == 0) {
            throw new IllegalArgumentException("Video vacío o nulo");
        }
        lastVideoFps = Math.max(1, sampleFps);
        lastVideoStop = stopOnFirstAbove;
        ensureVidWorker(lastVideoFps, lastVideoStop);
        String line = askWorker(vidWriter, vidReader, videoBytes, 60_000);
        return parseVideoResponse(line);
    }

    /* ============== Workers lifecycle ============== */

    private synchronized void ensureImgWorker() throws IOException {
        if (imgProc != null && imgProc.isAlive()) return;
        startImgWorker();
    }

    private synchronized void ensureVidWorker(int fps, boolean stopOnFirstAbove) throws IOException {
        if (vidProc != null && vidProc.isAlive()) return;
        startVidWorker(fps, stopOnFirstAbove);
    }

    private void startImgWorker() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("python", IMG_WORKER);
        pb.redirectErrorStream(false);
        imgProc = pb.start();
        imgReader = new BufferedReader(new InputStreamReader(imgProc.getInputStream(), StandardCharsets.UTF_8));
        imgWriter = new BufferedOutputStream(imgProc.getOutputStream());
    }

    private void startVidWorker(int fps, boolean stop) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("python", VID_WORKER, "--fps=" + fps, "--stop=" + (stop ? "1" : "0"));
        pb.redirectErrorStream(false);
        vidProc = pb.start();
        vidReader = new BufferedReader(new InputStreamReader(vidProc.getInputStream(), StandardCharsets.UTF_8));
        vidWriter = new BufferedOutputStream(vidProc.getOutputStream());
    }

    @SneakyThrows
    private String askWorker(BufferedOutputStream writer, BufferedReader reader, byte[] payload, int timeoutMs) {
        synchronized (this) {

            java.util.function.Supplier<String> sendAndRead = () -> {
                try {
                    byte[] len = ByteBuffer.allocate(4).putInt(payload.length).array();
                    writer.write(len);
                    writer.write(payload);
                    writer.flush();

                    long deadline = System.currentTimeMillis() + timeoutMs;
                    String line;
                    while (true) {
                        if (System.currentTimeMillis() > deadline) {
                            throw new IOException("Timeout leyendo respuesta del worker");
                        }
                        line = reader.readLine();
                        if (line == null) throw new IOException("Worker cerró stdout");
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        if (line.startsWith("{") && line.endsWith("}")) return line;

                        int comma = line.indexOf(',');
                        if (comma > 0) {
                            String tail = line.substring(comma + 1).trim();
                            try {
                                Double.parseDouble(tail);
                                return line;
                            } catch (NumberFormatException ignore) {}
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };

            try {
                return sendAndRead.get();
            } catch (RuntimeException first) {
                restartIfNeeded(writer);
                return sendAndRead.get();
            }
        }
    }

    private void restartIfNeeded(BufferedOutputStream writer) throws IOException {
        if (Objects.equals(writer, imgWriter)) {
            closeImgWorker();
            startImgWorker();
        } else if (Objects.equals(writer, vidWriter)) {
            closeVidWorker();
            startVidWorker(lastVideoFps, lastVideoStop);
        }
    }

    private void closeImgWorker() {
        safeClose(imgWriter);
        safeClose(imgReader);
        destroy(imgProc);
        imgWriter = null; imgReader = null; imgProc = null;
    }

    private void closeVidWorker() {
        safeClose(vidWriter);
        safeClose(vidReader);
        destroy(vidProc);
        vidWriter = null; vidReader = null; vidProc = null;
    }

    private void safeClose(Closeable c) { if (c != null) try { c.close(); } catch (IOException ignored) {} }
    private void destroy(Process p) { if (p != null) { p.destroy(); try { p.waitFor(); } catch (InterruptedException ignored) {} } }

    @PreDestroy
    public void shutdown() { closeImgWorker(); closeVidWorker(); }

    /* ============== Parsers ============== */

    private BirdDetectResponse parseImageResponse(String line) {
        try {
            JsonNode n = MAPPER.readTree(line);
            return BirdDetectResponse.builder()
                    .label(n.path("label").asText())
                    .trustLevel(n.path("trustLevel").asDouble())
                    .build();
        } catch (Exception ignore) {
            int comma = line.indexOf(',');
            if (comma <= 0) throw new RuntimeException("Salida IMG inesperada: " + line);
            String label = line.substring(0, comma).trim();
            String scoreStr = line.substring(comma + 1).trim();
            return BirdDetectResponse.builder()
                    .label(label)
                    .trustLevel(Double.parseDouble(scoreStr))
                    .build();
        }
    }

    @SneakyThrows
    private BirdVideoDetectResponse parseVideoResponse(String line) {
        JsonNode n = MAPPER.readTree(line);
        return BirdVideoDetectResponse.builder()
                .label(n.path("label").asText())
                .trustLevel(n.path("trustLevel").asDouble())
                .build();
    }
}