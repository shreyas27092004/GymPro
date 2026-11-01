package com.gympro.plan.exception;

/**
 * Thrown when a member already has an ACTIVE subscription and attempts to
 * subscribe to another plan directly (Problem #1, #8). Members with an
 * active plan must use the upgrade flow (POST /plans/upgrade) instead.
 * Mapped to 400 BAD REQUEST by GlobalExceptionHandler.
 */
public class DuplicateActiveSubscriptionException extends RuntimeException {
    public DuplicateActiveSubscriptionException(String message) {
        super(message);
    }
}
