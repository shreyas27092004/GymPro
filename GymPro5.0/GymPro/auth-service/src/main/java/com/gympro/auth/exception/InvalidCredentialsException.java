package com.gympro.auth.exception;

// Thrown when the supplied password does not match the stored hash
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials: password does not match");
    }
}
