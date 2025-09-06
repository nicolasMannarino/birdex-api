package com.birdex.utils;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class FilenameGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    private FilenameGenerator() {}

    public static String generate(String email, String birdName, String mimeType) {
        String safeBird = birdName == null || birdName.isBlank()
                ? "unknown"
                : birdName.trim().replaceAll("\\s+", "_");

        String ext = extensionFromMime(mimeType);

        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DATE_FMT);
        String time = now.format(TIME_FMT);
        String rand = UUID.randomUUID().toString().substring(0, 8);

        return String.format("%s/%s/%s_%s_%s%s", email, safeBird, date, time, rand, ext);
    }

    public static String extensionFromMime(String mime) {
        if (mime == null) return ".bin";
        String m = mime.toLowerCase();
        if (m.contains("audio/mpeg") || m.contains("audio/mp3")) return ".mp3";
        if (m.contains("audio/wav") || m.contains("audio/x-wav")) return ".wav";
        if (m.contains("audio/ogg")) return ".ogg";
        if (m.contains("audio/flac")) return ".flac";
        if (m.contains("image/jpeg")) return ".jpg";
        if (m.contains("image/png")) return ".png";
        if (m.contains("video/mp4")) return ".mp4";
        return ".bin";
    }
}