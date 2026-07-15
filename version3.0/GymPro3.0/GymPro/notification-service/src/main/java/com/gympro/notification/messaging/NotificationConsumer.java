package com.gympro.notification.messaging;

import com.gympro.notification.dto.NotificationEvent;
import com.gympro.notification.service.EmailService;
import com.gympro.notification.service.InAppNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * NotificationConsumer — listens on all GymPro notification queues.
 *
 * For EVERY event received, this consumer does TWO things:
 *   1. Sends email via EmailService
 *   2. Persists + SSE-pushes an InApp DB record via InAppNotificationService
 *
 * Queues:
 *   gympro.booking.queue  → BOOKING_CONFIRMED, BOOKING_TO_TRAINER, BOOKING_CANCELLED,
 *                           TRAINER_ASSIGNED, SESSION_REMINDER
 *   gympro.payment.queue  → PAYMENT_SUCCESS, PAYMENT_REFUND, PLAN_ACTIVATED
 *   gympro.otp.queue      → OTP_REQUESTED  (no in-app record – OTP is email-only)
 *
 * Error handling:
 *   - Transient errors → throw RuntimeException → retry interceptor (3x with backoff)
 *   - Permanent errors → throw AmqpRejectAndDontRequeueException → DLQ
 *
 * Deduplication:
 *   - InAppNotificationService.createNotification() checks userId+eventType+referenceId.
 *   - If duplicate, it silently skips saving (idempotent).
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @Autowired private EmailService emailService;
    @Autowired private InAppNotificationService inAppService;

    // ════════════════════════════════════════════════════════════════════════
    //  BOOKING QUEUE
    // ════════════════════════════════════════════════════════════════════════

    @RabbitListener(
        queues = "#{@bookingQueue}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleBookingEvent(@Payload NotificationEvent event) {
        log.info("📨 [BookingQueue] type={}, recipient={}", event.getEventType(), event.getRecipientEmail());
        validateEvent(event);

        try {
            switch (event.getEventType()) {

                case "BOOKING_CONFIRMED":
                    safeSendEmail("BOOKING_CONFIRMED", () -> emailService.sendBookingConfirmationToMember(
                        event.getRecipientEmail(), event.getBookingId(),
                        nullSafe(event.getTrainerName(), "Your Trainer"),
                        nullSafe(event.getSessionDay(), "TBD"),
                        nullSafe(event.getSessionTime(), "TBD")));
                    saveInApp(event.getUserId(),
                        nullSafe(event.getUserRole(), "MEMBER"),
                        "Session Confirmed ✅",
                        String.format("Your session with %s on %s at %s has been confirmed.",
                            nullSafe(event.getTrainerName(), "your trainer"),
                            nullSafe(event.getSessionDay(), "TBD"),
                            nullSafe(event.getSessionTime(), "TBD")),
                        "BOOKING", "BOOKING_CONFIRMED", event.getBookingId());
                    broadcastToAdmins(event.getAdminUserIds(),
                        "New Booking 📅",
                        String.format("Booking #%s confirmed for %s on %s at %s.",
                            event.getBookingId(),
                            nullSafe(event.getTrainerName(), "a trainer"),
                            nullSafe(event.getSessionDay(), "TBD"),
                            nullSafe(event.getSessionTime(), "TBD")),
                        "BOOKING", "ADMIN_NEW_BOOKING", event.getBookingId());
                    break;

                case "BOOKING_TO_TRAINER":
                    safeSendEmail("BOOKING_TO_TRAINER", () -> emailService.sendBookingNotificationToTrainer(
                        event.getRecipientEmail(), event.getBookingId(),
                        nullSafe(event.getMemberName(), "A Member"),
                        nullSafe(event.getSessionDay(), "TBD"),
                        nullSafe(event.getSessionTime(), "TBD")));
                    saveInApp(event.getUserId(), "TRAINER",
                        "New Booking 📅",
                        String.format("%s booked a session on %s at %s.",
                            nullSafe(event.getMemberName(), "A member"),
                            nullSafe(event.getSessionDay(), "TBD"),
                            nullSafe(event.getSessionTime(), "TBD")),
                        "BOOKING", "BOOKING_TO_TRAINER", event.getBookingId());
                    break;

                case "BOOKING_CANCELLED":
                    safeSendEmail("BOOKING_CANCELLED", () -> emailService.sendCancellationEmail(
                        event.getRecipientEmail(), event.getBookingId()));
                    saveInApp(event.getUserId(),
                        nullSafe(event.getUserRole(), "MEMBER"),
                        "Booking Cancelled ❌",
                        String.format("Booking #%s has been cancelled.", event.getBookingId()),
                        "BOOKING", "BOOKING_CANCELLED", event.getBookingId());
                    broadcastToAdmins(event.getAdminUserIds(),
                        "Booking Cancelled ❌",
                        String.format("Booking #%s has been cancelled.", event.getBookingId()),
                        "BOOKING", "ADMIN_BOOKING_CANCELLED", event.getBookingId());
                    break;

                case "TRAINER_ASSIGNED":
                    // No email template exists yet — in-app only
                    saveInApp(event.getUserId(),
                        nullSafe(event.getUserRole(), "MEMBER"),
                        "Trainer Assigned 💪",
                        String.format("Trainer %s has been assigned to you.",
                            nullSafe(event.getTrainerName(), "your trainer")),
                        "BOOKING", "TRAINER_ASSIGNED", event.getBookingId());
                    break;

                case "SESSION_REMINDER":
                    // In-app reminder (email handled by a scheduled job separately)
                    saveInApp(event.getUserId(),
                        nullSafe(event.getUserRole(), "MEMBER"),
                        "Session Reminder ⏰",
                        String.format("Reminder: Your session with %s is on %s at %s.",
                            nullSafe(event.getTrainerName(), "your trainer"),
                            nullSafe(event.getSessionDay(), "TBD"),
                            nullSafe(event.getSessionTime(), "TBD")),
                        "BOOKING", "SESSION_REMINDER", event.getBookingId());
                    break;

                default:
                    log.error("❌ [BookingQueue] Unknown eventType '{}' — DLQ", event.getEventType());
                    throw new AmqpRejectAndDontRequeueException("Unknown booking eventType: " + event.getEventType());
            }

            log.info("✅ [BookingQueue] Processed: type={}", event.getEventType());

        } catch (AmqpRejectAndDontRequeueException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [BookingQueue] Failed: type={} | {}", event.getEventType(), e.getMessage());
            throw new RuntimeException("Failed to process booking notification — will retry", e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAYMENT QUEUE
    // ════════════════════════════════════════════════════════════════════════

    @RabbitListener(
        queues = "#{@paymentQueue}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentEvent(@Payload NotificationEvent event) {
        log.info("📨 [PaymentQueue] type={}, recipient={}", event.getEventType(), event.getRecipientEmail());
        validateEvent(event);

        try {
            switch (event.getEventType()) {

                case "PAYMENT_SUCCESS":
                    safeSendEmail("PAYMENT_SUCCESS", () -> emailService.sendPaymentReceipt(
                        event.getRecipientEmail(), event.getAmount(),
                        nullSafe(event.getMethod(), "N/A"),
                        nullSafe(event.getTxnId(), "N/A"),
                        nullSafe(event.getDescription(), "GymPro Payment")));
                    saveInApp(event.getUserId(),
                        nullSafe(event.getUserRole(), "MEMBER"),
                        "Payment Successful 💳",
                        String.format("₹%.2f paid via %s. Txn: %s",
                            event.getAmount(),
                            nullSafe(event.getMethod(), "N/A"),
                            nullSafe(event.getTxnId(), "N/A")),
                        "PAYMENT", "PAYMENT_SUCCESS", event.getBookingId());
                    broadcastToAdmins(event.getAdminUserIds(),
                        "Payment Received 💳",
                        String.format("₹%.2f received via %s. Txn: %s",
                            event.getAmount(),
                            nullSafe(event.getMethod(), "N/A"),
                            nullSafe(event.getTxnId(), "N/A")),
                        "PAYMENT", "ADMIN_PAYMENT_RECEIVED", event.getBookingId());
                    break;

                case "PAYMENT_REFUND":
                    safeSendEmail("PAYMENT_REFUND", () -> emailService.sendRefundEmail(
                        event.getRecipientEmail(), event.getAmount(),
                        nullSafe(event.getTxnId(), "N/A")));
                    saveInApp(event.getUserId(),
                        nullSafe(event.getUserRole(), "MEMBER"),
                        "Refund Processed 💰",
                        String.format("₹%.2f refunded. Txn: %s",
                            event.getAmount(), nullSafe(event.getTxnId(), "N/A")),
                        "PAYMENT", "PAYMENT_REFUND", event.getBookingId());
                    broadcastToAdmins(event.getAdminUserIds(),
                        "Refund Processed 💰",
                        String.format("₹%.2f refunded. Txn: %s",
                            event.getAmount(), nullSafe(event.getTxnId(), "N/A")),
                        "PAYMENT", "ADMIN_REFUND_PROCESSED", event.getBookingId());
                    break;

                case "PLAN_ACTIVATED":
                    safeSendEmail("PLAN_ACTIVATED", () -> emailService.sendPlanSubscriptionEmail(
                        event.getRecipientEmail(),
                        nullSafe(event.getPlanName(), "Your Plan"),
                        nullSafe(event.getStartDate(), "N/A"),
                        nullSafe(event.getEndDate(), "N/A")));
                    saveInApp(event.getUserId(),
                        nullSafe(event.getUserRole(), "MEMBER"),
                        "Plan Activated 🎉",
                        String.format("%s activated. Valid: %s → %s",
                            nullSafe(event.getPlanName(), "Your Plan"),
                            nullSafe(event.getStartDate(), "N/A"),
                            nullSafe(event.getEndDate(), "N/A")),
                        "PLAN", "PLAN_ACTIVATED", null);
                    break;

                default:
                    log.error("❌ [PaymentQueue] Unknown eventType '{}' — DLQ", event.getEventType());
                    throw new AmqpRejectAndDontRequeueException("Unknown payment eventType: " + event.getEventType());
            }

            log.info("✅ [PaymentQueue] Processed: type={}", event.getEventType());

        } catch (AmqpRejectAndDontRequeueException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [PaymentQueue] Failed: type={} | {}", event.getEventType(), e.getMessage());
            throw new RuntimeException("Failed to process payment notification — will retry", e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  OTP QUEUE  (email-only — no in-app records)
    // ════════════════════════════════════════════════════════════════════════

    @RabbitListener(
        queues = "#{@otpQueue}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleOtpEvent(@Payload NotificationEvent event) {
        log.info("📨 [OtpQueue] type={}", event.getEventType());
        validateEvent(event);

        try {
            if ("OTP_REQUESTED".equals(event.getEventType())) {
                if (event.getOtp() == null || event.getOtp().isBlank()) {
                    throw new AmqpRejectAndDontRequeueException("OTP field is null or blank");
                }
                emailService.sendOtpEmail(event.getRecipientEmail(), event.getOtp());
                // OTPs intentionally NOT stored as in-app notifications (security)
            } else {
                throw new AmqpRejectAndDontRequeueException("Unknown OTP eventType: " + event.getEventType());
            }
            log.info("✅ [OtpQueue] OTP email sent to: {}", event.getRecipientEmail());

        } catch (AmqpRejectAndDontRequeueException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [OtpQueue] Failed: recipient={} | {}", event.getRecipientEmail(), e.getMessage());
            throw new RuntimeException("Failed to send OTP notification — will retry", e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Save in-app notification only if userId is present.
     * Passes eventType + referenceId for deduplication.
     */
    private void saveInApp(Long userId, String userRole, String title,
                            String message, String type, String eventType, Long referenceId) {
        if (userId == null) {
            log.warn("⚠️  saveInApp skipped: userId is null for eventType={}", eventType);
            return;
        }
        inAppService.createNotification(userId, userRole, title, message, type, eventType, referenceId);
    }

    /**
     * Run an email-sending action without letting a failure (bad address,
     * SMTP outage, template error, etc.) propagate and block the in-app
     * save that follows it in each switch-case. Email and in-app delivery
     * are two independent channels — one going down must never take the
     * other down with it. Logged, not swallowed silently.
     */
    private void safeSendEmail(String context, Runnable emailAction) {
        try {
            emailAction.run();
        } catch (Exception e) {
            log.warn("⚠️  Email send failed for {} (in-app notification will still be saved): {}",
                      context, e.getMessage());
        }
    }

    /**
     * Broadcast an in-app notification to every admin user ID attached to
     * the event. Each admin gets their own row (own dedup key), and a
     * failure for one admin never blocks the others.
     */
    private void broadcastToAdmins(java.util.List<Long> adminUserIds, String title, String message,
                                    String type, String eventType, Long referenceId) {
        if (adminUserIds == null || adminUserIds.isEmpty()) return;
        for (Long adminId : adminUserIds) {
            if (adminId == null) continue;
            try {
                inAppService.createNotification(adminId, "ADMIN", title, message, type, eventType, referenceId);
            } catch (Exception e) {
                log.warn("⚠️  Failed to create admin broadcast notification for adminId={}: {}",
                          adminId, e.getMessage());
            }
        }
    }

    private void validateEvent(NotificationEvent event) {
        if (event == null)
            throw new AmqpRejectAndDontRequeueException("Received null NotificationEvent");
        if (event.getRecipientEmail() == null || event.getRecipientEmail().isBlank())
            throw new AmqpRejectAndDontRequeueException(
                "NotificationEvent has no recipientEmail (eventType=" + event.getEventType() + ")");
        if (event.getEventType() == null || event.getEventType().isBlank())
            throw new AmqpRejectAndDontRequeueException("NotificationEvent has no eventType");
    }

    private String nullSafe(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}