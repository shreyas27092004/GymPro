package com.gympro.booking.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for booking-service's local copy of {@link NotificationEvent}.
 * Covers every static factory method, the all-args constructor, all getters/setters,
 * and toString.
 */
class NotificationEventTest {

    @Test
    @DisplayName("No-args constructor creates an empty event")
    void noArgsConstructor_createsEmptyEvent() {
        NotificationEvent event = new NotificationEvent();
        assertThat(event.getEventType()).isNull();
    }

    @Test
    @DisplayName("All-args constructor sets every field")
    void allArgsConstructor_setsAllFields() {
        NotificationEvent event = new NotificationEvent(
                "CUSTOM", "x@y.com", 1L, 99.99, "000000",
                "Trainer", "Member", "MON", "09:00",
                "desc", "UPI", "txn-1", "Plan", "2026-01-01", "2026-02-01");

        assertThat(event.getEventType()).isEqualTo("CUSTOM");
        assertThat(event.getRecipientEmail()).isEqualTo("x@y.com");
        assertThat(event.getBookingId()).isEqualTo(1L);
        assertThat(event.getAmount()).isEqualTo(99.99);
        assertThat(event.getOtp()).isEqualTo("000000");
        assertThat(event.getTrainerName()).isEqualTo("Trainer");
        assertThat(event.getMemberName()).isEqualTo("Member");
        assertThat(event.getSessionDay()).isEqualTo("MON");
        assertThat(event.getSessionTime()).isEqualTo("09:00");
        assertThat(event.getDescription()).isEqualTo("desc");
        assertThat(event.getMethod()).isEqualTo("UPI");
        assertThat(event.getTxnId()).isEqualTo("txn-1");
        assertThat(event.getPlanName()).isEqualTo("Plan");
        assertThat(event.getStartDate()).isEqualTo("2026-01-01");
        assertThat(event.getEndDate()).isEqualTo("2026-02-01");
    }

    @Test
    @DisplayName("bookingConfirmed factory sets expected fields")
    void bookingConfirmed_setsFields() {
        NotificationEvent event = NotificationEvent.bookingConfirmed(
                "m@x.com", 1L, 100L, "Coach Amy", "MON", "09:00");

        assertThat(event.getEventType()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(event.getUserId()).isEqualTo(1L);
        assertThat(event.getUserRole()).isEqualTo("MEMBER");
        assertThat(event.getBookingId()).isEqualTo(100L);
        assertThat(event.getTrainerName()).isEqualTo("Coach Amy");
    }

    @Test
    @DisplayName("bookingToTrainer factory sets expected fields")
    void bookingToTrainer_setsFields() {
        NotificationEvent event = NotificationEvent.bookingToTrainer(
                "t@x.com", 2L, 101L, "John Member", "TUE", "10:00");

        assertThat(event.getEventType()).isEqualTo("BOOKING_TO_TRAINER");
        assertThat(event.getUserId()).isEqualTo(2L);
        assertThat(event.getUserRole()).isEqualTo("TRAINER");
        assertThat(event.getMemberName()).isEqualTo("John Member");
    }

    @Test
    @DisplayName("bookingCancelled factory sets expected fields")
    void bookingCancelled_setsFields() {
        NotificationEvent event = NotificationEvent.bookingCancelled("m@x.com", 3L, 102L);

        assertThat(event.getEventType()).isEqualTo("BOOKING_CANCELLED");
        assertThat(event.getUserId()).isEqualTo(3L);
        assertThat(event.getUserRole()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("paymentSuccess factory sets expected fields")
    void paymentSuccess_setsFields() {
        NotificationEvent event = NotificationEvent.paymentSuccess(
                "m@x.com", 4L, 199.99, "CARD", "txn-2", "fee");

        assertThat(event.getEventType()).isEqualTo("PAYMENT_SUCCESS");
        assertThat(event.getAmount()).isEqualTo(199.99);
        assertThat(event.getMethod()).isEqualTo("CARD");
    }

    @Test
    @DisplayName("paymentRefund factory sets expected fields")
    void paymentRefund_setsFields() {
        NotificationEvent event = NotificationEvent.paymentRefund("m@x.com", 5L, 50.0, "txn-3");

        assertThat(event.getEventType()).isEqualTo("PAYMENT_REFUND");
        assertThat(event.getAmount()).isEqualTo(50.0);
        assertThat(event.getTxnId()).isEqualTo("txn-3");
    }

    @Test
    @DisplayName("otpRequested factory sets expected fields")
    void otpRequested_setsFields() {
        NotificationEvent event = NotificationEvent.otpRequested("m@x.com", "654321");

        assertThat(event.getEventType()).isEqualTo("OTP_REQUESTED");
        assertThat(event.getOtp()).isEqualTo("654321");
    }

    @Test
    @DisplayName("planActivated factory sets expected fields")
    void planActivated_setsFields() {
        NotificationEvent event = NotificationEvent.planActivated(
                "m@x.com", 6L, "Premium", "2026-01-01", "2026-12-31");

        assertThat(event.getEventType()).isEqualTo("PLAN_ACTIVATED");
        assertThat(event.getPlanName()).isEqualTo("Premium");
    }

    @Test
    @DisplayName("Setters update adminUserIds and all remaining fields")
    void setters_updateAllFields() {
        NotificationEvent event = new NotificationEvent();

        event.setAdminUserIds(List.of(1L, 2L));
        event.setRecipientEmail("z@z.com");

        assertThat(event.getAdminUserIds()).containsExactly(1L, 2L);
        assertThat(event.getRecipientEmail()).isEqualTo("z@z.com");
    }

    @Test
    @DisplayName("toString includes key identifying fields")
    void toString_includesKeyFields() {
        NotificationEvent event = NotificationEvent.bookingConfirmed(
                "m@x.com", 1L, 100L, "Coach", "MON", "09:00");

        assertThat(event.toString()).contains("BOOKING_CONFIRMED", "m@x.com", "100");
    }
}
