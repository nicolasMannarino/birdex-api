package com.birdex.exception;

public class UserNotFoundException extends RuntimeException{
    private final String email;

    public UserNotFoundException(String email) {
        super("User not found for email: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
