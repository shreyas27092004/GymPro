package com.gympro.notification.controller;

import com.gympro.notification.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationController controller;

    // ─── /notify/send ─────────────────────────────────────────────────────

    @Test
    void testSend_Success() {
        doNothing().when(emailService).sendEmail("user@test.com", "Hello", "World");

        ResponseEntity<String> response = controller.send("user@test.com", "Hello", "World");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("user@test.com"));
        verify(emailService, times(1)).sendEmail("user@test.com", "Hello", "World");
    }

    @Test
    void testSend_ServiceThrows_PropagatesException() {
        doThrow(new IllegalArgumentException("Invalid email"))
            .when(emailService).sendEmail(anyString(), anyString(), anyString());

        assertThrows(IllegalArgumentException.class, () ->
            controller.send("bad", "Subject", "Body")
        );
    }

    // ─── /notify/booking/member ───────────────────────────────────────────

    @Test
    void testBookingMember_Success() {
        doNothing().when(emailService).sendBookingConfirmationToMember(
            "member@test.com", 1L, "Ravi", "MON", "09:00-11:00");

        ResponseEntity<String> response = controller.bookingMember(
            "member@test.com", 1L, "Ravi", "MON", "09:00-11:00");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("member@test.com"));
    }

    // ─── /notify/booking/trainer ──────────────────────────────────────────

    @Test
    void testBookingTrainer_Success() {
        doNothing().when(emailService).sendBookingNotificationToTrainer(
            "trainer@test.com", 2L, "Member A", "TUE", "10:00-12:00");

        ResponseEntity<String> response = controller.bookingTrainer(
            "trainer@test.com", 2L, "Member A", "TUE", "10:00-12:00");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("trainer@test.com"));
    }

    // ─── /notify/booking/cancel ───────────────────────────────────────────

    @Test
    void testBookingCancel_Success() {
        doNothing().when(emailService).sendCancellationEmail("user@test.com", 3L);

        ResponseEntity<String> response = controller.bookingCancel("user@test.com", 3L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("user@test.com"));
        verify(emailService, times(1)).sendCancellationEmail("user@test.com", 3L);
    }

    // ─── /notify/payment/receipt ──────────────────────────────────────────

    @Test
    void testPaymentReceipt_Success() {
        doNothing().when(emailService).sendPaymentReceipt(
            "payer@test.com", 999.0, "UPI", "TXN-001", "Booking #5");

        ResponseEntity<String> response = controller.paymentReceipt(
            "payer@test.com", 999.0, "UPI", "TXN-001", "Booking #5");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("payer@test.com"));
    }

    @Test
    void testPaymentReceipt_ServiceThrows_PropagatesException() {
        doThrow(new IllegalArgumentException("Bad email"))
            .when(emailService).sendPaymentReceipt(anyString(), anyDouble(),
                anyString(), anyString(), anyString());

        assertThrows(IllegalArgumentException.class, () ->
            controller.paymentReceipt("bad", 100.0, "UPI", "TXN-001", "desc")
        );
    }

    // ─── /notify/payment/refund ───────────────────────────────────────────

    @Test
    void testPaymentRefund_Success() {
        doNothing().when(emailService).sendRefundEmail("user@test.com", 500.0, "TXN-REF001");

        ResponseEntity<String> response = controller.paymentRefund("user@test.com", 500.0, "TXN-REF001");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("user@test.com"));
    }

    // ─── /notify/plan/subscribed ──────────────────────────────────────────

    @Test
    void testPlanSubscribed_Success() {
        doNothing().when(emailService).sendPlanSubscriptionEmail(
            "member@test.com", "Premium", "2025-04-01", "2026-04-01");

        ResponseEntity<String> response = controller.planSubscribed(
            "member@test.com", "Premium", "2025-04-01", "2026-04-01");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("member@test.com"));
        verify(emailService, times(1)).sendPlanSubscriptionEmail(
            "member@test.com", "Premium", "2025-04-01", "2026-04-01");
    }

    // ─── /notify/test ─────────────────────────────────────────────────────

    @Test
    void testSendOtpEndpoint_Success() {
        doNothing().when(emailService).sendOtpEmail("user@test.com", "654321");

        ResponseEntity<String> response = controller.sendOtp("user@test.com", "654321");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("user@test.com"));
        verify(emailService, times(1)).sendOtpEmail("user@test.com", "654321");
    }

    @Test
    void testHealthEndpoint() {
        String result = controller.test();
        assertNotNull(result);
        assertTrue(result.contains("Notification Service Working"));
    }
}
