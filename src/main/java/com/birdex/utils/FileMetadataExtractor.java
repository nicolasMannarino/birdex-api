package com.birdex.utils;

import java.util.Base64;

public class FileMetadataExtractor {
    private FileMetadataExtractor() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String extractMimeType(String base64Data) {
        if (base64Data == null || !base64Data.startsWith("data:")) return "application/octet-stream";
        int start = base64Data.indexOf(":") + 1;
        int end = base64Data.indexOf(";");
        return base64Data.substring(start, end);
    }

    public static byte[] extractData(String base64Data) {
        String base64Cleaned = base64Data.replaceFirst("^data:[^;]+;base64,", "");
        return Base64.getDecoder().decode(base64Cleaned);
    }
}