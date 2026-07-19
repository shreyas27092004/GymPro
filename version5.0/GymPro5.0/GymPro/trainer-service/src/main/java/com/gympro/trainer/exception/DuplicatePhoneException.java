package com.gympro.trainer.exception;

/**
 * Thrown when a trainer is created or updated with a phone number that is
 * already registered against another trainer record.
 */
public class DuplicatePhoneException extends RuntimeException {

    public DuplicatePhoneException(String phone) {
        super("Phone number " + phone + " is already registered with another trainer");
    }
}