package com.birdex.entity.enums;

public enum MissionType {
    DAILY, WEEKLY, UNIQUE;

    public static MissionType fromDb(String raw) {
        if (raw == null) return UNIQUE;
        return switch (raw.trim().toLowerCase()) {
            case "daily" -> DAILY;
            case "weekly" -> WEEKLY;
            default -> UNIQUE;
        };
    }

    public String toDb() {
        return switch (this) {
            case DAILY -> "daily";
            case WEEKLY -> "weekly";
            case UNIQUE -> "unique";
        };
    }
}
