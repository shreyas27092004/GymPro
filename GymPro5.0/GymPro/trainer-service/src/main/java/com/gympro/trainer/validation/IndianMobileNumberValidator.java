package com.gympro.trainer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validates phone numbers against Indian mobile number rules and attaches a
 * specific, meaningful message for each failure reason instead of a single
 * generic message.
 */
public class IndianMobileNumberValidator implements ConstraintValidator<IndianMobileNumber, String> {

    // Exactly 10 digits, first digit 6-9. Anchored so no country code / extra
    // digits / symbols can sneak in (e.g. "+919876543210" or "919876543210" fail).
    private static final Pattern INDIAN_MOBILE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("^[0-9]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Blank/null is allowed here — trainer profiles are auto-created at
        // self-registration before a phone number is collected. Format is
        // only enforced once a value is actually supplied (e.g. admin/trainer
        // fills it in later via update).
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        String trimmed = value.trim();

        if (!DIGITS_ONLY_PATTERN.matcher(trimmed).matches()) {
            addViolation(context, "Phone number must contain digits only (letters and special characters are not allowed)");
            return false;
        }

        if (trimmed.length() != 10) {
            addViolation(context, "Phone number must be exactly 10 digits (do not include a country code, spaces, or symbols)");
            return false;
        }

        if (!INDIAN_MOBILE_PATTERN.matcher(trimmed).matches()) {
            addViolation(context, "Phone number must start with 6, 7, 8, or 9");
            return false;
        }

        return true;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}