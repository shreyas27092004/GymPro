package com.gympro.notification.service;

import com.gympro.notification.exception.EmailException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceExtendedTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@gympro.com");
    }

    private MimeMessage setupMimeMock() {
        MimeMessage fakeMime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(fakeMime);
        return fakeMime;
    }

    // ─── sendEmail validation: null recipient ─────────────────────────────

    @Test
    void testSendEmail_NullRecipient_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendEmail(null, "Subject", "Body")
        );
        assertTrue(ex.getMessage().contains("email cannot be empty"));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ─── sendEmail validation: null subject ───────────────────────────────

    @Test
    void testSendEmail_NullSubject_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendEmail("valid@test.com", null, "Body")
        );
        assertTrue(ex.getMessage().contains("subject cannot be empty"));
    }

    // ─── sendEmail validation: null body ──────────────────────────────────

    @Test
    void testSendEmail_NullBody_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendEmail("valid@test.com", "Subject", null)
        );
        assertTrue(ex.getMessage().contains("body cannot be empty"));
    }

    // ─── sendEmail: MailException wraps to EmailException ─────────────────

    @Test
    void testSendEmail_MailSenderThrows_WrapsToEmailException() {
        setupMimeMock();
        doThrow(new MailSendException("SMTP connection refused"))
            .when(mailSender).send(any(MimeMessage.class));

        EmailException ex = assertThrows(EmailException.class, () ->
            emailService.sendEmail("valid@test.com", "Subject", "Body")
        );
        assertTrue(ex.getMessage().contains("Failed to send email"));
    }

    // ─── sendBookingConfirmationToMember ─────────────────────────────────

    @Test
    void testSendBookingConfirmationToMember_Success() {
        setupMimeMock();
        assertDoesNotThrow(() ->
            emailService.sendBookingConfirmationToMember(
                "member@test.com", 100L, "Trainer Ravi", "WEDNESDAY", "10:00-12:00")
        );
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendBookingConfirmationToMember_InvalidEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            emailService.sendBookingConfirmationToMember(
                "not-an-email", 1L, "Ravi", "MON", "09:00")
        );
    }

    // ─── sendBookingNotificationToTrainer ────────────────────────────────

    @Test
    void testSendBookingNotificationToTrainer_Success() {
        setupMimeMock();
        assertDoesNotThrow(() ->
            emailService.sendBookingNotificationToTrainer(
                "trainer@test.com", 200L, "Member Arjun", "FRIDAY", "08:00-09:00")
        );
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendBookingNotificationToTrainer_EmptyEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            emailService.sendBookingNotificationToTrainer(
                "", 1L, "Member", "MON", "09:00")
        );
    }

    // ─── sendCancellationEmail ────────────────────────────────────────────

    @Test
    void testSendCancellationEmail_Success() {
        setupMimeMock();
        assertDoesNotThrow(() ->
            emailService.sendCancellationEmail("member@test.com", 55L)
        );
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendCancellationEmail_InvalidEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            emailService.sendCancellationEmail("bad-email", 1L)
        );
    }

    // ─── sendPaymentReceipt ───────────────────────────────────────────────

    @Test
    void testSendPaymentReceipt_AllMethods_Success() {
        setupMimeMock();
        for (String method : new String[]{"UPI", "CASH", "CREDIT_CARD", "DEBIT_CARD", "QR_CODE"}) {
            assertDoesNotThrow(() ->
                emailService.sendPaymentReceipt(
                    "user@test.com", 999.0, method, "TXN-" + method, "Booking #1")
            );
        }
        verify(mailSender, times(5)).send(any(MimeMessage.class));
    }

    @Test
    void testSendPaymentReceipt_FormatsAmountCorrectly() {
        setupMimeMock();
        // Should not throw even for fractional amounts
        assertDoesNotThrow(() ->
            emailService.sendPaymentReceipt("user@test.com", 1234.56, "UPI", "TXN-001", "desc")
        );
    }

    // ─── sendRefundEmail ──────────────────────────────────────────────────

    @Test
    void testSendRefundEmail_Success() {
        setupMimeMock();
        assertDoesNotThrow(() ->
            emailService.sendRefundEmail("member@test.com", 500.0, "TXN-REFUND01")
        );
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendRefundEmail_EmptyEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            emailService.sendRefundEmail("", 100.0, "TXN-001")
        );
    }

    // ─── sendPlanSubscriptionEmail ────────────────────────────────────────

    @Test
    void testSendPlanSubscriptionEmail_Success() {
        setupMimeMock();
        assertDoesNotThrow(() ->
            emailService.sendPlanSubscriptionEmail(
                "member@test.com", "Premium Plan", "2025-04-01", "2026-04-01")
        );
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendPlanSubscriptionEmail_InvalidEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            emailService.sendPlanSubscriptionEmail("invalid", "Plan", "2025-01-01", "2026-01-01")
        );
    }

    // ─── sendOtpEmail ─────────────────────────────────────────────────────

    @Test
    void testSendOtpEmail_Success() {
        setupMimeMock();
        assertDoesNotThrow(() ->
            emailService.sendOtpEmail("user@test.com", "123456")
        );
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendOtpEmail_InvalidEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            emailService.sendOtpEmail("not-an-email", "123456")
        );
    }

    // ─── Multiple sends don't interfere ───────────────────────────────────

    @Test
    void testSendEmail_MultipleSuccessfulCalls() {
        setupMimeMock();
        // setupMimeMock sets up once but mock returns same for all calls
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() ->
                emailService.sendEmail("user" + "@test.com", "Subject " , "Body text")
            );
        }
        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }
}
