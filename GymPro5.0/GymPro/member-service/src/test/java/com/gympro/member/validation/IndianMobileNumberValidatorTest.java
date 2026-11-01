package com.gympro.member.validation;

import com.gympro.member.entity.Member;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the @IndianMobileNumber constraint end-to-end through the real
 * jakarta.validation.Validator (as Spring MVC would invoke it via @Valid),
 * rather than calling the ConstraintValidator directly.
 */
class IndianMobileNumberValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    private Member memberWithPhone(String phone) {
        Member member = new Member();
        member.setName("Test User");
        member.setEmail("test@example.com");
        member.setPhone(phone);
        return member;
    }

    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "6000000000", "7123456789", "8999999999"})
    void validIndianMobileNumbers_ShouldPassValidation(String phone) {
        Set<ConstraintViolation<Member>> violations = validator.validate(memberWithPhone(phone));
        assertTrue(violations.isEmpty(), "Expected no violations for valid phone: " + phone);
    }

    @Test
    void phoneStartingWithInvalidDigit_ShouldFailWithStartingDigitMessage() {
        Set<ConstraintViolation<Member>> violations = validator.validate(memberWithPhone("5876543210"));

        assertEquals(1, violations.size());
        assertEquals("Phone number must start with 6, 7, 8, or 9",
                violations.iterator().next().getMessage());
    }

    @Test
    void tooShortPhone_ShouldFailWithLengthMessage() {
        Set<ConstraintViolation<Member>> violations = validator.validate(memberWithPhone("12345"));

        assertEquals(1, violations.size());
        assertEquals("Phone number must be exactly 10 digits (do not include a country code, spaces, or symbols)",
                violations.iterator().next().getMessage());
    }

    @Test
    void alphabeticPhone_ShouldFailWithDigitsOnlyMessage() {
        Set<ConstraintViolation<Member>> violations = validator.validate(memberWithPhone("abcdefghij"));

        assertEquals(1, violations.size());
        assertEquals("Phone number must contain digits only (letters and special characters are not allowed)",
                violations.iterator().next().getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankPhone_ShouldFailWithBlankMessage(String phone) {
        Set<ConstraintViolation<Member>> violations = validator.validate(memberWithPhone(phone));

        assertEquals(1, violations.size());
        assertEquals("Phone number must not be blank", violations.iterator().next().getMessage());
    }

    @Test
    void nullPhone_ShouldFailWithBlankMessage() {
        Set<ConstraintViolation<Member>> violations = validator.validate(memberWithPhone(null));

        assertEquals(1, violations.size());
        assertEquals("Phone number must not be blank", violations.iterator().next().getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "+919876543210",  // country code with plus
        "919876543210",   // country code without plus
        "98765432100",    // 11 digits
        "98-76543210",    // special character
        "987654321 "      // trailing space still only 9 digits before it
    })
    void phoneWithCountryCodeOrSpecialCharacters_ShouldFailValidation(String phone) {
        Set<ConstraintViolation<Member>> violations = validator.validate(memberWithPhone(phone));
        assertFalse(violations.isEmpty(), "Expected violation for: " + phone);
    }
}
