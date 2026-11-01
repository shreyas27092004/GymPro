package com.gympro.trainer.exception;

public class TrainerNotFoundException extends RuntimeException {

    public TrainerNotFoundException(Long id) {
        super("Trainer not found with id: " + id);
    }

    public TrainerNotFoundException(String message) {
        super(message);
    }
}
