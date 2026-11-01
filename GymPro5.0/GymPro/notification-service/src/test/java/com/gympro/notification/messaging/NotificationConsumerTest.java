package com.gympro.notification.messaging;

import com.gympro.notification.dto.NotificationEvent;
import com.gympro.notification.service.EmailService;
import com.gympro.notification.service.InAppNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationConsumer}.
 * {@link EmailService} and {@link InAppNotificationService} are mocked — no real
 * email or database work happens.
 */
@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private InAppNotificationService inAppService;

    private NotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer();
        ReflectionTestUtils.setField(consumer, "emailService", emailService);
        ReflectionTestUtils.setField(consumer, "inAppService", inAppService);
    }

    // ══════════════════════════════════════════════════════════════════
    // Booking queue
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleBookingEvent")
    class BookingEventTests {

        @Test
        @DisplayName("BOOKING_CONFIRMED sends email, saves in-app record, and broadcasts to admins")
        void bookingConfirmed_fullFlow() {
            NotificationEvent event = NotificationEvent.bookingConfirmed(
                    "member@x.com", 1L, 100L, "Coach Amy", "MON", "09:00-10:00");
            event.setAdminUserIds(List.of(50L, 51L));

            consumer.handleBookingEvent(event);

            verify(emailService, times(1)).sendBookingConfirmationToMember(
                    "member@x.com", 100L, "Coach Amy", "MON", "09:00-10:00");
            verify(inAppService, times(1)).createNotification(
                    eq(1L), eq("MEMBER"), anyString(), anyString(), eq("BOOKING"), eq("BOOKING_CONFIRMED"), eq(100L));
            verify(inAppService, times(1)).createNotification(
                    eq(50L), eq("ADMIN"), anyString(), anyString(), eq("BOOKING"), eq("ADMIN_NEW_BOOKING"), eq(100L));
            verify(inAppService, times(1)).createNotification(
                    eq(51L), eq("ADMIN"), anyString(), anyString(), eq("BOOKING"), eq("ADMIN_NEW_BOOKING"), eq(100L));
        }

        @Test
        @DisplayName("BOOKING_CONFIRMED with null optional fields falls back to defaults (nullSafe false branch)")
        void bookingConfirmed_nullOptionalFields_usesDefaults() {
            NotificationEvent event = new NotificationEvent();
            event.setEventType("BOOKING_CONFIRMED");
            event.setRecipientEmail("member@x.com");
            event.setUserId(1L);
            event.setBookingId(100L);
            // trainerName, sessionDay, sessionTime, userRole intentionally left null

            consumer.handleBookingEvent(event);

            verify(emailService).sendBookingConfirmationToMember(
                    "member@x.com", 100L, "Your Trainer", "TBD", "TBD");
            verify(inAppService).createNotification(
                    eq(1L), eq("MEMBER"), anyString(), anyString(), eq("BOOKING"), eq("BOOKING_CONFIRMED"), eq(100L));
        }

        @Test
        @DisplayName("BOOKING_CONFIRMED continues saving in-app record even when email sending fails")
        void bookingConfirmed_emailFails_stillSavesInApp() {
            NotificationEvent event = NotificationEvent.bookingConfirmed(
                    "member@x.com", 1L, 100L, "Coach Amy", "MON", "09:00");
            doThrow(new RuntimeException("SMTP down"))
                    .when(emailService).sendBookingConfirmationToMember(any(), any(), any(), any(), any());

            consumer.handleBookingEvent(event);

            verify(inAppService).createNotification(
                    eq(1L), anyString(), anyString(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("BOOKING_TO_TRAINER sends email and saves in-app record for the trainer")
        void bookingToTrainer_fullFlow() {
            NotificationEvent event = NotificationEvent.bookingToTrainer(
                    "trainer@x.com", 2L, 101L, "John Member", "TUE", "10:00-11:00");

            consumer.handleBookingEvent(event);

            verify(emailService, times(1)).sendBookingNotificationToTrainer(
                    "trainer@x.com", 101L, "John Member", "TUE", "10:00-11:00");
            verify(inAppService, times(1)).createNotification(
                    eq(2L), eq("TRAINER"), anyString(), anyString(), eq("BOOKING"), eq("BOOKING_TO_TRAINER"), eq(101L));
        }

        @Test
        @DisplayName("BOOKING_CANCELLED sends email, saves in-app record, and broadcasts to admins")
        void bookingCancelled_fullFlow() {
            NotificationEvent event = NotificationEvent.bookingCancelled("member@x.com", 3L, 102L);
            event.setAdminUserIds(List.of(50L));

            consumer.handleBookingEvent(event);

            verify(emailService, times(1)).sendCancellationEmail("member@x.com", 102L);
            verify(inAppService, times(1)).createNotification(
                    eq(3L), eq("MEMBER"), anyString(), anyString(), eq("BOOKING"), eq("BOOKING_CANCELLED"), eq(102L));
            verify(inAppService, times(1)).createNotification(
                    eq(50L), eq("ADMIN"), anyString(), anyString(), eq("BOOKING"), eq("ADMIN_BOOKING_CANCELLED"), eq(102L));
        }

        @Test
        @DisplayName("TRAINER_ASSIGNED saves an in-app record only — no email")
        void trainerAssigned_inAppOnly() {
            NotificationEvent event = NotificationEvent.trainerAssigned("member@x.com", 4L, 103L, "Coach Bob");

            consumer.handleBookingEvent(event);

            verify(inAppService, times(1)).createNotification(
                    eq(4L), eq("MEMBER"), anyString(), anyString(), eq("BOOKING"), eq("TRAINER_ASSIGNED"), eq(103L));
            verify(emailService, never()).sendBookingConfirmationToMember(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("SESSION_REMINDER saves an in-app record only — no email")
        void sessionReminder_inAppOnly() {
            NotificationEvent event = NotificationEvent.sessionReminder(
                    "member@x.com", 5L, "MEMBER", 104L, "Coach Amy", "WED", "11:00");

            consumer.handleBookingEvent(event);

            verify(inAppService, times(1)).createNotification(
                    eq(5L), eq("MEMBER"), anyString(), anyString(), eq("BOOKING"), eq("SESSION_REMINDER"), eq(104L));
        }

        @Test
        @DisplayName("saveInApp is skipped when userId is null")
        void saveInApp_nullUserId_isSkipped() {
            NotificationEvent event = NotificationEvent.bookingCancelled("member@x.com", null, 102L);

            consumer.handleBookingEvent(event);

            verify(inAppService, never()).createNotification(
                    isNull(), anyString(), anyString(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("broadcastToAdmins is a no-op when adminUserIds is null")
        void broadcastToAdmins_nullList_noOp() {
            NotificationEvent event = NotificationEvent.bookingConfirmed(
                    "member@x.com", 1L, 100L, "Coach Amy", "MON", "09:00");
            event.setAdminUserIds(null);

            consumer.handleBookingEvent(event);

            verify(inAppService, never()).createNotification(
                    anyLong(), eq("ADMIN"), anyString(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("broadcastToAdmins is a no-op when adminUserIds is empty")
        void broadcastToAdmins_emptyList_noOp() {
            NotificationEvent event = NotificationEvent.bookingConfirmed(
                    "member@x.com", 1L, 100L, "Coach Amy", "MON", "09:00");
            event.setAdminUserIds(List.of());

            consumer.handleBookingEvent(event);

            verify(inAppService, never()).createNotification(
                    anyLong(), eq("ADMIN"), anyString(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("broadcastToAdmins skips null admin IDs but still notifies the valid ones")
        void broadcastToAdmins_skipsNullIds() {
            NotificationEvent event = NotificationEvent.bookingConfirmed(
                    "member@x.com", 1L, 100L, "Coach Amy", "MON", "09:00");
            event.setAdminUserIds(Arrays.asList(50L, null));

            consumer.handleBookingEvent(event);

            verify(inAppService, times(1)).createNotification(
                    eq(50L), eq("ADMIN"), anyString(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("broadcastToAdmins continues to remaining admins when one throws")
        void broadcastToAdmins_oneFails_othersStillNotified() {
            NotificationEvent event = NotificationEvent.bookingConfirmed(
                    "member@x.com", 1L, 100L, "Coach Amy", "MON", "09:00");
            event.setAdminUserIds(List.of(50L, 51L));
            doThrow(new RuntimeException("db error"))
                    .when(inAppService).createNotification(eq(50L), eq("ADMIN"), anyString(), anyString(), anyString(), anyString(), any());

            consumer.handleBookingEvent(event);

            verify(inAppService, times(1)).createNotification(
                    eq(51L), eq("ADMIN"), anyString(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Unknown booking eventType throws AmqpRejectAndDontRequeueException")
        void unknownEventType_throwsRejectException() {
            NotificationEvent event = NotificationEvent.bookingCancelled("member@x.com", 1L, 1L);
            event.setEventType("SOMETHING_UNKNOWN");

            assertThatThrownBy(() -> consumer.handleBookingEvent(event))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }

        @Test
        @DisplayName("Unexpected exception during processing is wrapped in a retryable RuntimeException")
        void unexpectedException_wrappedForRetry() {
            NotificationEvent event = NotificationEvent.bookingConfirmed(
                    "member@x.com", 1L, 100L, "Coach Amy", "MON", "09:00");
            doThrow(new RuntimeException("db down"))
                    .when(inAppService).createNotification(eq(1L), anyString(), anyString(), anyString(), anyString(), anyString(), any());

            assertThatThrownBy(() -> consumer.handleBookingEvent(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process booking notification");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Payment queue
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handlePaymentEvent")
    class PaymentEventTests {

        @Test
        @DisplayName("PAYMENT_SUCCESS sends email, saves in-app record, and broadcasts to admins")
        void paymentSuccess_fullFlow() {
            NotificationEvent event = NotificationEvent.paymentSuccess(
                    "member@x.com", 6L, 499.99, "UPI", "txn-1", "Session fee");
            event.setAdminUserIds(List.of(50L));

            consumer.handlePaymentEvent(event);

            verify(emailService, times(1)).sendPaymentReceipt(
                    "member@x.com", 499.99, "UPI", "txn-1", "Session fee");
            verify(inAppService, times(1)).createNotification(
                    eq(6L), eq("MEMBER"), anyString(), anyString(), eq("PAYMENT"), eq("PAYMENT_SUCCESS"), any());
            verify(inAppService, times(1)).createNotification(
                    eq(50L), eq("ADMIN"), anyString(), anyString(), eq("PAYMENT"), eq("ADMIN_PAYMENT_RECEIVED"), any());
        }

        @Test
        @DisplayName("PAYMENT_REFUND sends email, saves in-app record, and broadcasts to admins")
        void paymentRefund_fullFlow() {
            NotificationEvent event = NotificationEvent.paymentRefund("member@x.com", 7L, 250.0, "txn-2");
            event.setAdminUserIds(List.of(50L));

            consumer.handlePaymentEvent(event);

            verify(emailService, times(1)).sendRefundEmail("member@x.com", 250.0, "txn-2");
            verify(inAppService, times(1)).createNotification(
                    eq(7L), eq("MEMBER"), anyString(), anyString(), eq("PAYMENT"), eq("PAYMENT_REFUND"), any());
            verify(inAppService, times(1)).createNotification(
                    eq(50L), eq("ADMIN"), anyString(), anyString(), eq("PAYMENT"), eq("ADMIN_REFUND_PROCESSED"), any());
        }

        @Test
        @DisplayName("PLAN_ACTIVATED sends email and saves an in-app record with a null referenceId")
        void planActivated_fullFlow() {
            NotificationEvent event = NotificationEvent.planActivated(
                    "member@x.com", 8L, "Premium", "2026-01-01", "2026-12-31");

            consumer.handlePaymentEvent(event);

            verify(emailService, times(1)).sendPlanSubscriptionEmail(
                    "member@x.com", "Premium", "2026-01-01", "2026-12-31");
            verify(inAppService, times(1)).createNotification(
                    eq(8L), eq("MEMBER"), anyString(), anyString(), eq("PLAN"), eq("PLAN_ACTIVATED"), isNull());
        }

        @Test
        @DisplayName("REFUND_REQUESTED emails the member and broadcasts 'Refund Request Received' to admins")
        void refundRequested_fullFlow() {
            NotificationEvent event = NotificationEvent.refundRequested(
                    "member@x.com", 300.0, "txn-3", 42L, "Trainer never showed up");
            event.setAdminUserIds(List.of(50L, 51L));

            consumer.handlePaymentEvent(event);

            verify(emailService, times(1)).sendRefundRequestReceivedEmail("member@x.com", 300.0, "txn-3");
            verify(inAppService, times(1)).createNotification(
                    eq(50L), eq("ADMIN"), anyString(), anyString(), eq("PAYMENT"), eq("ADMIN_REFUND_REQUESTED"), eq(42L));
            verify(inAppService, times(1)).createNotification(
                    eq(51L), eq("ADMIN"), anyString(), anyString(), eq("PAYMENT"), eq("ADMIN_REFUND_REQUESTED"), eq(42L));
            // No in-app record for the member themselves — only the admin broadcast + confirmation email.
            verify(inAppService, never()).createNotification(
                    eq(50L), eq("MEMBER"), anyString(), anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("REFUND_APPROVED emails and saves an in-app record for the member")
        void refundApproved_fullFlow() {
            NotificationEvent event = NotificationEvent.refundApproved(
                    "member@x.com", 9L, 300.0, "txn-3", 42L);

            consumer.handlePaymentEvent(event);

            verify(emailService, times(1)).sendRefundApprovedEmail("member@x.com", 300.0, "txn-3");
            verify(inAppService, times(1)).createNotification(
                    eq(9L), eq("MEMBER"), anyString(), anyString(), eq("PAYMENT"), eq("REFUND_APPROVED"), eq(42L));
        }

        @Test
        @DisplayName("REFUND_REJECTED emails the reason and saves an in-app record for the member")
        void refundRejected_fullFlow() {
            NotificationEvent event = NotificationEvent.refundRejected(
                    "member@x.com", 9L, 300.0, "txn-3", 42L, "No evidence provided");

            consumer.handlePaymentEvent(event);

            verify(emailService, times(1)).sendRefundRejectedEmail(
                    "member@x.com", 300.0, "txn-3", "No evidence provided");
            verify(inAppService, times(1)).createNotification(
                    eq(9L), eq("MEMBER"), anyString(), anyString(), eq("PAYMENT"), eq("REFUND_REJECTED"), eq(42L));
        }

        @Test
        @DisplayName("REFUND_COMPLETED emails and notifies both member and admins")
        void refundCompleted_fullFlow() {
            NotificationEvent event = NotificationEvent.refundCompleted(
                    "member@x.com", 9L, 300.0, "txn-3", 42L);
            event.setAdminUserIds(List.of(50L));

            consumer.handlePaymentEvent(event);

            verify(emailService, times(1)).sendRefundEmail("member@x.com", 300.0, "txn-3");
            verify(inAppService, times(1)).createNotification(
                    eq(9L), eq("MEMBER"), anyString(), anyString(), eq("PAYMENT"), eq("REFUND_COMPLETED"), eq(42L));
            verify(inAppService, times(1)).createNotification(
                    eq(50L), eq("ADMIN"), anyString(), anyString(), eq("PAYMENT"), eq("ADMIN_REFUND_COMPLETED"), eq(42L));
        }

        @Test
        @DisplayName("Unknown payment eventType throws AmqpRejectAndDontRequeueException")
        void unknownEventType_throwsRejectException() {
            NotificationEvent event = NotificationEvent.paymentRefund("member@x.com", 7L, 1.0, "t");
            event.setEventType("SOMETHING_UNKNOWN");

            assertThatThrownBy(() -> consumer.handlePaymentEvent(event))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }

        @Test
        @DisplayName("Unexpected exception during processing is wrapped in a retryable RuntimeException")
        void unexpectedException_wrappedForRetry() {
            NotificationEvent event = NotificationEvent.paymentSuccess(
                    "member@x.com", 6L, 100.0, "UPI", "t1", "d");
            doThrow(new RuntimeException("db down"))
                    .when(inAppService).createNotification(eq(6L), anyString(), anyString(), anyString(), anyString(), anyString(), any());

            assertThatThrownBy(() -> consumer.handlePaymentEvent(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process payment notification");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // OTP queue
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("handleOtpEvent")
    class OtpEventTests {

        @Test
        @DisplayName("OTP_REQUESTED with a valid otp sends the email")
        void otpRequested_validOtp_sendsEmail() {
            NotificationEvent event = NotificationEvent.otpRequested("member@x.com", "123456");

            consumer.handleOtpEvent(event);

            verify(emailService, times(1)).sendOtpEmail("member@x.com", "123456");
            verify(inAppService, never()).createNotification(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("OTP_REQUESTED with a null otp throws AmqpRejectAndDontRequeueException")
        void otpRequested_nullOtp_throwsRejectException() {
            NotificationEvent event = NotificationEvent.otpRequested("member@x.com", null);

            assertThatThrownBy(() -> consumer.handleOtpEvent(event))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }

        @Test
        @DisplayName("OTP_REQUESTED with a blank otp throws AmqpRejectAndDontRequeueException")
        void otpRequested_blankOtp_throwsRejectException() {
            NotificationEvent event = NotificationEvent.otpRequested("member@x.com", "   ");

            assertThatThrownBy(() -> consumer.handleOtpEvent(event))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }

        @Test
        @DisplayName("Unknown OTP eventType throws AmqpRejectAndDontRequeueException")
        void unknownEventType_throwsRejectException() {
            NotificationEvent event = NotificationEvent.otpRequested("member@x.com", "123456");
            event.setEventType("SOMETHING_ELSE");

            assertThatThrownBy(() -> consumer.handleOtpEvent(event))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }

        @Test
        @DisplayName("Unexpected exception during OTP send is wrapped in a retryable RuntimeException")
        void unexpectedException_wrappedForRetry() {
            NotificationEvent event = NotificationEvent.otpRequested("member@x.com", "123456");
            doThrow(new RuntimeException("smtp down")).when(emailService).sendOtpEmail(any(), any());

            assertThatThrownBy(() -> consumer.handleOtpEvent(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send OTP notification");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // validateEvent (exercised via all three listeners)
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validateEvent")
    class ValidateEventTests {

        @Test
        @DisplayName("Null event throws AmqpRejectAndDontRequeueException")
        void nullEvent_throwsRejectException() {
            assertThatThrownBy(() -> consumer.handleBookingEvent(null))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }

        @Test
        @DisplayName("Null recipientEmail throws AmqpRejectAndDontRequeueException")
        void nullRecipientEmail_throwsRejectException() {
            NotificationEvent event = NotificationEvent.bookingCancelled(null, 1L, 1L);

            assertThatThrownBy(() -> consumer.handleBookingEvent(event))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }

        @Test
        @DisplayName("Blank recipientEmail throws AmqpRejectAndDontRequeueException")
        void blankRecipientEmail_throwsRejectException() {
            NotificationEvent event = NotificationEvent.bookingCancelled("   ", 1L, 1L);

            assertThatThrownBy(() -> consumer.handleBookingEvent(event))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }

        @Test
        @DisplayName("Null eventType throws AmqpRejectAndDontRequeueException")
        void nullEventType_throwsRejectException() {
            NotificationEvent event = NotificationEvent.bookingCancelled("member@x.com", 1L, 1L);
            event.setEventType(null);

            assertThatThrownBy(() -> consumer.handleBookingEvent(event))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }

        @Test
        @DisplayName("Blank eventType throws AmqpRejectAndDontRequeueException")
        void blankEventType_throwsRejectException() {
            NotificationEvent event = NotificationEvent.bookingCancelled("member@x.com", 1L, 1L);
            event.setEventType("   ");

            assertThatThrownBy(() -> consumer.handleBookingEvent(event))
                    .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        }
    }
}
