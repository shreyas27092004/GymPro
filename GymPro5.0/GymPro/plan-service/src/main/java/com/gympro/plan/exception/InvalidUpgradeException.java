package com.gympro.plan.exception;

/**
 * Thrown for upgrade requests that are invalid for reasons other than
 * "downgrade" — e.g. no active subscription to upgrade from, target plan
 * is inactive, or target plan is the same as the current plan.
 * Mapped to 400 BAD REQUEST by GlobalExceptionHandler.
 */
public class InvalidUpgradeException extends RuntimeException {
    public InvalidUpgradeException(String message) {
        super(message);
    }
}
