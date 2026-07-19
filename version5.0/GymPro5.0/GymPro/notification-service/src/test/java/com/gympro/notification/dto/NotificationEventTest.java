package com.gympro.notification.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NotificationEvent}.
 * Covers every static factory helper, all getters/setters, and toString.
 */
class NotificationEventTest {

    @Test
    @DisplayName("No-args constructor creates an empty event")
    void noArgsConstructor_createsEmptyEvent() {
        NotificationEvent event = new NotificationEvent();
        assertThat(event.getEventType()).isNull();
        assertThat(event.getRecipientEmail()).isNull();
    }

    @Test
    @DisplayName("bookingConfirmed factory sets all expected fields")
    void bookingConfirmed_setsFields() {
        NotificationEvent event = NotificationEvent.bookingConfirmed(
                "member@x.com", 1L, 100L, "Coach Amy", "MON", "09:00-10:00");

        assertThat(event.getEventType()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(event.getRecipientEmail()).isEqualTo("member@x.com");
        assertThat(event.getUserId()).isEqualTo(1L);
        assertThat(event.getUserRole()).isEqualTo("MEMBER");
        assertThat(event.getBookingId()).isEqualTo(100L);
        assertThat(event.getTrainerName()).isEqualTo("Coach Amy");
        assertThat(event.getSessionDay()).isEqualTo("MON");
        assertThat(event.getSessionTime()).isEqualTo("09:00-10:00");
    }

    @Test
    @DisplayName("bookingToTrainer factory sets all expected fields")
    void bookingToTrainer_setsFields() {
        NotificationEvent event = NotificationEvent.bookingToTrainer(
                "trainer@x.com", 2L, 101L, "John Member", "TUE", "10:00-11:00");

        assertThat(event.getEventType()).isEqualTo("BOOKING_TO_TRAINER");
        assertThat(event.getRecipientEmail()).isEqualTo("trainer@x.com");
        assertThat(event.getUserId()).isEqualTo(2L);
        assertThat(event.getUserRole()).isEqualTo("TRAINER");
        assertThat(event.getBookingId()).isEqualTo(101L);
        assertThat(event.getMemberName()).isEqualTo("John Member");
        assertThat(event.getSessionDay()).isEqualTo("TUE");
        assertThat(event.getSessionTime()).isEqualTo("10:00-11:00");
    }

    @Test
    @DisplayName("bookingCancelled factory sets all expected fields")
    void bookingCancelled_setsFields() {
        NotificationEvent event = NotificationEvent.bookingCancelled("member@x.com", 3L, 102L);

        assertThat(event.getEventType()).isEqualTo("BOOKING_CANCELLED");
        assertThat(event.getRecipientEmail()).isEqualTo("member@x.com");
        assertThat(event.getUserId()).isEqualTo(3L);
        assertThat(event.getUserRole()).isEqualTo("MEMBER");
        assertThat(event.getBookingId()).isEqualTo(102L);
    }

    @Test
    @DisplayName("trainerAssigned factory sets all expected fields")
    void trainerAssigned_setsFields() {
        NotificationEvent event = NotificationEvent.trainerAssigned("member@x.com", 4L, 103L, "Coach Bob");

        assertThat(event.getEventType()).isEqualTo("TRAINER_ASSIGNED");
        assertThat(event.getRecipientEmail()).isEqualTo("member@x.com");
        assertThat(event.getUserId()).isEqualTo(4L);
        assertThat(event.getUserRole()).isEqualTo("MEMBER");
        assertThat(event.getBookingId()).isEqualTo(103L);
        assertThat(event.getTrainerName()).isEqualTo("Coach Bob");
    }

    @Test
    @DisplayName("sessionReminder factory sets all expected fields including custom role")
    void sessionReminder_setsFields() {
        NotificationEvent event = NotificationEvent.sessionReminder(
                "trainer@x.com", 5L, "TRAINER", 104L, "Coach Amy", "WED", "11:00-12:00");

        assertThat(event.getEventType()).isEqualTo("SESSION_REMINDER");
        assertThat(event.getRecipientEmail()).isEqualTo("trainer@x.com");
        assertThat(event.getUserId()).isEqualTo(5L);
        assertThat(event.getUserRole()).isEqualTo("TRAINER");
        assertThat(event.getBookingId()).isEqualTo(104L);
        assertThat(event.getTrainerName()).isEqualTo("Coach Amy");
        assertThat(event.getSessionDay()).isEqualTo("WED");
        assertThat(event.getSessionTime()).isEqualTo("11:00-12:00");
    }

    @Test
    @DisplayName("paymentSuccess factory sets all expected fields")
    void paymentSuccess_setsFields() {
        NotificationEvent event = NotificationEvent.paymentSuccess(
                "member@x.com", 6L, 499.99, "UPI", "txn-1", "Session fee");

        assertThat(event.getEventType()).isEqualTo("PAYMENT_SUCCESS");
        assertThat(event.getRecipientEmail()).isEqualTo("member@x.com");
        assertThat(event.getUserId()).isEqualTo(6L);
        assertThat(event.getUserRole()).isEqualTo("MEMBER");
        assertThat(event.getAmount()).isEqualTo(499.99);
        assertThat(event.getMethod()).isEqualTo("UPI");
        assertThat(event.getTxnId()).isEqualTo("txn-1");
        assertThat(event.getDescription()).isEqualTo("Session fee");
    }

    @Test
    @DisplayName("paymentRefund factory sets all expected fields")
    void paymentRefund_setsFields() {
        NotificationEvent event = NotificationEvent.paymentRefund("member@x.com", 7L, 250.0, "txn-2");

        assertThat(event.getEventType()).isEqualTo("PAYMENT_REFUND");
        assertThat(event.getRecipientEmail()).isEqualTo("member@x.com");
        assertThat(event.getUserId()).isEqualTo(7L);
        assertThat(event.getUserRole()).isEqualTo("MEMBER");
        assertThat(event.getAmount()).isEqualTo(250.0);
        assertThat(event.getTxnId()).isEqualTo("txn-2");
    }

    @Test
    @DisplayName("refundRequested factory sets all expected fields")
    void refundRequested_setsFields() {
        NotificationEvent event = NotificationEvent.refundRequested(
                "member@x.com", 250.0, "txn-2", 42L, "Trainer never showed up");

        assertThat(event.getEventType()).isEqualTo("REFUND_REQUESTED");
        assertThat(event.getRecipientEmail()).isEqualTo("member@x.com");
        assertThat(event.getAmount()).isEqualTo(250.0);
        assertThat(event.getTxnId()).isEqualTo("txn-2");
        assertThat(event.getPaymentId()).isEqualTo(42L);
        assertThat(event.getDescription()).isEqualTo("Trainer never showed up");
    }

    @Test
    @DisplayName("refundApproved factory sets all expected fields")
    void refundApproved_setsFields() {
        NotificationEvent event = NotificationEvent.refundApproved(
                "member@x.com", 9L, 250.0, "txn-2", 42L);

        assertThat(event.getEventType()).isEqualTo("REFUND_APPROVED");
        assertThat(event.getRecipientEmail()).isEqualTo("member@x.com");
        assertThat(event.getUserId()).isEqualTo(9L);
        assertThat(event.getUserRole()).isEqualTo("MEMBER");
        assertThat(event.getAmount()).isEqualTo(250.0);
        assertThat(event.getTxnId()).isEqualTo("txn-2");
        assertThat(event.getPaymentId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("refundRejected factory sets all expected fields")
    void refundRejected_setsFields() {
        NotificationEvent event = NotificationEvent.refundRejected(
                "member@x.com", 9L, 250.0, "txn-2", 42L, "No evidence provided");

        assertThat(event.getEventType()).isEqualTo("REFUND_REJECTED");
        assertThat(event.getUserId()).isEqualTo(9L);
        assertThat(event.getPaymentId()).isEqualTo(42L);
        assertThat(event.getDescription()).isEqualTo("No evidence provided");
    }

    @Test
    @DisplayName("refundCompleted factory sets all expected fields")
    void refundCompleted_setsFields() {
        NotificationEvent event = NotificationEvent.refundCompleted(
                "member@x.com", 9L, 250.0, "txn-2", 42L);

        assertThat(event.getEventType()).isEqualTo("REFUND_COMPLETED");
        assertThat(event.getUserId()).isEqualTo(9L);
        assertThat(event.getUserRole()).isEqualTo("MEMBER");
        assertThat(event.getPaymentId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("planActivated factory sets all expected fields")
    void planActivated_setsFields() {
        NotificationEvent event = NotificationEvent.planActivated(
                "member@x.com", 8L, "Premium", "2026-01-01", "2026-12-31");

        assertThat(event.getEventType()).isEqualTo("PLAN_ACTIVATED");
        assertThat(event.getRecipientEmail()).isEqualTo("member@x.com");
        assertThat(event.getUserId()).isEqualTo(8L);
        assertThat(event.getUserRole()).isEqualTo("MEMBER");
        assertThat(event.getPlanName()).isEqualTo("Premium");
        assertThat(event.getStartDate()).isEqualTo("2026-01-01");
        assertThat(event.getEndDate()).isEqualTo("2026-12-31");
    }

    @Test
    @DisplayName("otpRequested factory sets all expected fields")
    void otpRequested_setsFields() {
        NotificationEvent event = NotificationEvent.otpRequested("member@x.com", "123456");

        assertThat(event.getEventType()).isEqualTo("OTP_REQUESTED");
        assertThat(event.getRecipientEmail()).isEqualTo("member@x.com");
        assertThat(event.getOtp()).isEqualTo("123456");
    }

    @Test
    @DisplayName("All setters update their corresponding fields")
    void setters_updateAllFields() {
        NotificationEvent event = new NotificationEvent();

        event.setEventType("CUSTOM");
        event.setRecipientEmail("x@y.com");
        event.setUserId(1L);
        event.setUserRole("ADMIN");
        event.setAdminUserIds(List.of(1L, 2L));
        event.setBookingId(10L);
        event.setTrainerName("Trainer");
        event.setTrainerId(20L);
        event.setMemberName("Member");
        event.setMemberId(30L);
        event.setSessionDay("FRI");
        event.setSessionTime("12:00");
        event.setAmount(99.99);
        event.setMethod("CARD");
        event.setTxnId("txn-x");
        event.setDescription("desc");
        event.setPlanName("Basic");
        event.setStartDate("2026-01-01");
        event.setEndDate("2026-02-01");
        event.setOtp("000000");

        assertThat(event.getEventType()).isEqualTo("CUSTOM");
        assertThat(event.getRecipientEmail()).isEqualTo("x@y.com");
        assertThat(event.getUserId()).isEqualTo(1L);
        assertThat(event.getUserRole()).isEqualTo("ADMIN");
        assertThat(event.getAdminUserIds()).containsExactly(1L, 2L);
        assertThat(event.getBookingId()).isEqualTo(10L);
        assertThat(event.getTrainerName()).isEqualTo("Trainer");
        assertThat(event.getTrainerId()).isEqualTo(20L);
        assertThat(event.getMemberName()).isEqualTo("Member");
        assertThat(event.getMemberId()).isEqualTo(30L);
        assertThat(event.getSessionDay()).isEqualTo("FRI");
        assertThat(event.getSessionTime()).isEqualTo("12:00");
        assertThat(event.getAmount()).isEqualTo(99.99);
        assertThat(event.getMethod()).isEqualTo("CARD");
        assertThat(event.getTxnId()).isEqualTo("txn-x");
        assertThat(event.getDescription()).isEqualTo("desc");
        assertThat(event.getPlanName()).isEqualTo("Basic");
        assertThat(event.getStartDate()).isEqualTo("2026-01-01");
        assertThat(event.getEndDate()).isEqualTo("2026-02-01");
        assertThat(event.getOtp()).isEqualTo("000000");
    }

    @Test
    @DisplayName("toString includes key identifying fields")
    void toString_includesKeyFields() {
        NotificationEvent event = NotificationEvent.bookingConfirmed(
                "member@x.com", 1L, 100L, "Coach Amy", "MON", "09:00");

        String result = event.toString();

        assertThat(result).contains("BOOKING_CONFIRMED", "member@x.com", "100");
    }
}
