package com.gympro.member.exception;

/**
 * Thrown when a member is created or updated with a phone number that is
 * already registered against another member record.
 */
public class DuplicatePhoneException extends RuntimeException {

    public DuplicatePhoneException(String phone) {
        super("Phone number " + phone + " is already registered with another member");
    }
}
