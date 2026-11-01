package com.gympro.plan.exception;

/**
 * Thrown when a member attempts to move to a plan whose configured
 * priorityLevel is not strictly greater than their current plan's
 * priorityLevel (Problem #2: Elite -> Premium, Premium -> Basic, etc.).
 * Mapped to 400 BAD REQUEST by GlobalExceptionHandler.
 */
public class PlanDowngradeNotAllowedException extends RuntimeException {
    public PlanDowngradeNotAllowedException(String message) {
        super(message);
    }
}
