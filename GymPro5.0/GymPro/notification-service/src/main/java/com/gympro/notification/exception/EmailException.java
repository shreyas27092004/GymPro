package com.gympro.notification.exception;

// Custom exception thrown when email sending fails
// Wraps the original mail exception with a clear message
public class EmailException extends RuntimeException {

    public EmailException(String message) {
        super(message);
    }

    public EmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
