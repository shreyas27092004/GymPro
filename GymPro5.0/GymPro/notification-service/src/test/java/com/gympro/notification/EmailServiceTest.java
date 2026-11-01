package com.gympro.notification;

import com.gympro.notification.exception.EmailException;
import com.gympro.notification.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// ✅ JUnit 5 + Mockito Tests for EmailService
//
// @ExtendWith(MockitoExtension.class)
//   → Tells JUnit to use Mockito extension (enables @Mock and @InjectMocks)
//
// @Mock
//   → Creates a FAKE version of JavaMailSender
//   → We don't want to send REAL emails during tests!
//   → Mockito simulates its behavior
//
// @InjectMocks
//   → Creates a REAL EmailService but injects the @Mock objects into it
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;     // FAKE mail sender (no real emails)

    @InjectMocks
    private EmailService emailService;     // REAL service with fake mail sender injected

    @BeforeEach
    void setUp() {
        // Inject the @Value field (fromEmail) manually since we're not loading Spring context
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@gmail.com");
    }

    // ─── TEST 1: Happy path – valid email sends successfully ─────────────
    @Test
    void testSendEmail_Success() throws Exception {
        // ARRANGE: Create a fake MimeMessage for the mock to return
        MimeMessage fakeMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(fakeMimeMessage);

        // ACT: Call sendEmail – should not throw any exception
        assertDoesNotThrow(() ->
            emailService.sendEmail("shreyas@gmail.com", "Test Subject", "Test Body")
        );

        // ASSERT: Verify that mailSender.send() was called exactly once
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ─── TEST 2: Empty email address throws IllegalArgumentException ──────
    @Test
    void testSendEmail_EmptyRecipient_ThrowsException() {
        // ACT + ASSERT: Sending to empty email should throw IllegalArgumentException
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendEmail("", "Subject", "Body")
        );

        // Verify the error message makes sense
        assertTrue(ex.getMessage().contains("email cannot be empty"));

        // Verify mailSender was NEVER called (validation stopped it)
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ─── TEST 3: Invalid email format throws IllegalArgumentException ─────
    @Test
    void testSendEmail_InvalidEmailFormat_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendEmail("not-an-email", "Subject", "Body")
        );
        assertTrue(ex.getMessage().contains("Invalid email address"));
    }

    // ─── TEST 4: Empty subject throws IllegalArgumentException ────────────
    @Test
    void testSendEmail_EmptySubject_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendEmail("test@gmail.com", "", "Body")
        );
        assertTrue(ex.getMessage().contains("subject cannot be empty"));
    }

    // ─── TEST 5: Empty body throws IllegalArgumentException ──────────────
    @Test
    void testSendEmail_EmptyBody_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendEmail("test@gmail.com", "Subject", "")
        );
        assertTrue(ex.getMessage().contains("body cannot be empty"));
    }

    // ─── TEST 6: Verify booking confirmation template works ──────────────
    @Test
    void testSendBookingConfirmation_Success() throws Exception {
        MimeMessage fakeMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(fakeMimeMessage);

        // ACT
        assertDoesNotThrow(() ->
            emailService.sendBookingConfirmationToMember(
                "member@gmail.com", 1L, "Ravi Trainer", "MON", "09:00-11:00")
        );

        // ASSERT: email was sent
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ─── TEST 7: Verify payment receipt template works ───────────────────
    @Test
    void testSendPaymentReceipt_Success() throws Exception {
        MimeMessage fakeMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(fakeMimeMessage);

        assertDoesNotThrow(() ->
            emailService.sendPaymentReceipt(
                "member@gmail.com", 999.0, "UPI", "TXN-ABC12345", "Booking #5")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
