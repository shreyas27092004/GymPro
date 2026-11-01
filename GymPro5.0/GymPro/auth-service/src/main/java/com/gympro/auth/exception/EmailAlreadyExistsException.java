package com.gympro.auth.exception;

// Thrown when a user tries to register with an already-used email
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
