package com.gympro.trainer.exception;

/**
 * Thrown when a trainer tries to create a session on a date + start time
 * that they already have a session scheduled for.
 */
public class DuplicateScheduleException extends RuntimeException {

    public DuplicateScheduleException(String message) {
        super(message);
    }
}
