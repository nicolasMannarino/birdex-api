package com.birdex.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugs {
    private Slugs() {}

    public static String of(String input) {
        if (input == null) return null;

        String s = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replace("ß", "ss")
                .replace("æ", "ae")
                .replace("Æ", "ae")
                .replace("œ", "oe")
                .replace("Œ", "oe");
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
    }
}