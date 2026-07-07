package com.gympro.auth.exception;

// Thrown when a login attempt references an email that doesn't exist
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String email) {
        super("User not found: " + email);
    }
}
