package com.gympro.trainer.exception;

public class ScheduleNotFoundException extends RuntimeException {

    public ScheduleNotFoundException(Long id) {
        super("Schedule slot not found with id: " + id);
    }

    public ScheduleNotFoundException(String message) {
        super(message);
    }
}
