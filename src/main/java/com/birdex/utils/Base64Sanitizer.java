package com.birdex.utils;

import java.util.Base64;

public final class Base64Sanitizer {
    private Base64Sanitizer() {}

    public static byte[] decode(String raw) {
        if (raw == null) throw new IllegalArgumentException("fileBase64 null");
        String s = raw.trim();
        int i = s.indexOf("base64,");
        if (i >= 0) s = s.substring(i + "base64,".length());
        s = s.replaceAll("\\s+", ""); // quita CR/LF/espacios
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("fileBase64 inválido", e);
        }
    }
}