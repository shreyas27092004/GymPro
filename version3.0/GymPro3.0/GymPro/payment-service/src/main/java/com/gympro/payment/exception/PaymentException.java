package com.gympro.payment.exception;

// Custom exception for payment-related errors
// Makes error handling more specific and readable
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
