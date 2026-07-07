package com.gympro.trainer.exception;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String role) {
        super("Access denied for role: " + role);
    }

    public AccessDeniedException() {
        super("Access denied: insufficient permissions");
    }
}
