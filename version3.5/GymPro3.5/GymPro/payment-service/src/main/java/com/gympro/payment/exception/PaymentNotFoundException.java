package com.gympro.payment.exception;

// Thrown when a payment record is not found in the database
// Results in 404 NOT FOUND response (handled by GlobalExceptionHandler)
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
