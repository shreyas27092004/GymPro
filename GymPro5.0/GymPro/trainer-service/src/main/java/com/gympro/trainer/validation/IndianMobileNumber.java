package com.gympro.trainer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enterprise-grade Bean Validation constraint for Indian mobile numbers.
 *
 * A valid value must be:
 *  - Not blank
 *  - Exactly 10 digits (no country code, spaces, "+", "-", or letters)
 *  - Starting with 6, 7, 8, or 9
 *
 * Examples: 9876543210 (valid), 5876543210 / 12345 / abcdefghij / +919876543210 (invalid)
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IndianMobileNumberValidator.class)
public @interface IndianMobileNumber {

    String message() default "Phone number must be a valid 10-digit Indian mobile number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}