package com.birdex.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class FilenameGenerator {

    private FilenameGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String generate(String userId, String originalName) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s/%s_%s_%s", userId, userId, timestamp, originalName);
    }
}
