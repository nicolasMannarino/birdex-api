package com.birdex.exception;

public class BirdNotFoundException extends RuntimeException {

    private final String birdName;
    private final Long birdId;

    public BirdNotFoundException(String birdName) {
        super("Bird not found for name: " + birdName);
        this.birdName = birdName;
        this.birdId = null;
    }

    public BirdNotFoundException(Long birdId) {
        super("Bird not found for id: " + birdId);
        this.birdId = birdId;
        this.birdName = null;
    }

    public String getBirdName() {
        return birdName;
    }

    public Long getBirdId() {
        return birdId;
    }
}
