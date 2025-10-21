package com.birdex.utils;


import java.text.Normalizer;
import java.util.Locale;

public final class Slugs {
    private Slugs() {
    }

    public static String of(String input) {
        return snake(input);
    }

    public static String snake(String input) {
        if (input == null) return null;
        String s = normalizeAscii(input);

        s = s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .replaceAll("_{2,}", "_");
        return s;
    }

    public static String kebab(String input) {
        if (input == null) return null;
        String s = normalizeAscii(input);
        s = s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
        return s;
    }

    private static String normalizeAscii(String input) {
        String s = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replace("ß", "ss")
                .replace("æ", "ae")
                .replace("Æ", "ae")
                .replace("œ", "oe")
                .replace("Œ", "oe");
        return s;
    }
}