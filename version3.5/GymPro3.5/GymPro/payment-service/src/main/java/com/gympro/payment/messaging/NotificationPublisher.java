package com.gympro.payment.messaging;
// ─────────────────────────────────────────────────────────────────────────────
//  NotificationPublisher – PRODUCER component (one per producer service)
//
//  Copy this file into:
//    booking-service  → package com.gympro.booking.messaging;   (keep as-is)
//    payment-service  → package com.gympro.payment.messaging;   (change package)
//    auth-service     → package com.gympro.auth.messaging;      (change package)
//
//  Also change the import of NotificationEvent to match the service's package:
//    booking-service  → import com.gympro.booking.dto.NotificationEvent;
//    payment-service  → import com.gympro.payment.dto.NotificationEvent;
//    auth-service     → import com.gympro.auth.dto.NotificationEvent;
//
//  HOW IT WORKS:
//    - publishBookingEvent() → sends to gympro.exchange with "booking.notification"
//    - publishPaymentEvent() → sends to gympro.exchange with "payment.notification"
//    - publishOtpEvent()     → sends to gympro.exchange with "auth.otp"
//    - notification-service consumes from the bound queues asynchronously
// ─────────────────────────────────────────────────────────────────────────────

import com.gympro.payment.config.RabbitMQConfig;
import com.gympro.payment.dto.NotificationEvent;         // ← change package per service
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // ─────────────────────────────────────────────────────────────────────
    //  BOOKING EVENTS
    //  Publishes to: gympro.exchange  /  routing key: booking.notification
    //  Consumed by:  gympro.booking.queue  in notification-service
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Publish any booking-related notification event.
     * Caller creates the event via NotificationEvent.bookingConfirmed(...)
     * or NotificationEvent.bookingCancelled(...) or bookingToTrainer(...).
     *
     * @param event the notification event to publish
     */
    public void publishBookingEvent(NotificationEvent event) {
        publish(event, RabbitMQConfig.EXCHANGE, RabbitMQConfig.BOOKING_ROUTING_KEY);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PAYMENT EVENTS
    //  Publishes to: gympro.exchange  /  routing key: payment.notification
    //  Consumed by:  gympro.payment.queue  in notification-service
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Publish any payment-related notification event.
     * Caller creates the event via NotificationEvent.paymentSuccess(...)
     * or NotificationEvent.paymentRefund(...) or planActivated(...).
     *
     * @param event the notification event to publish
     */
    public void publishPaymentEvent(NotificationEvent event) {
        publish(event, RabbitMQConfig.EXCHANGE, RabbitMQConfig.PAYMENT_ROUTING_KEY);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  OTP / AUTH EVENTS
    //  Publishes to: gympro.exchange  /  routing key: auth.otp
    //  Consumed by:  gympro.otp.queue  in notification-service
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Publish an OTP request event.
     * Caller creates the event via NotificationEvent.otpRequested(...).
     *
     * @param event the notification event to publish
     */
    public void publishOtpEvent(NotificationEvent event) {
        publish(event, RabbitMQConfig.EXCHANGE, RabbitMQConfig.OTP_ROUTING_KEY);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  INTERNAL HELPER
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Core publish method — converts the event to JSON and sends to RabbitMQ.
     * Logs success or failure. Does NOT re-throw so callers can treat
     * notification as best-effort (business operation already succeeded).
     */
    private void publish(NotificationEvent event, String exchange, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("✅ [RabbitMQ] Published event: type={}, recipient={}, exchange={}, key={}",
                     event.getEventType(), event.getRecipientEmail(), exchange, routingKey);
        } catch (AmqpException ex) {
            // RabbitMQ is down or connection refused — log and continue.
            // The business operation (booking / payment) already completed successfully.
            log.error("❌ [RabbitMQ] Failed to publish event: type={}, recipient={} | Error: {}",
                      event.getEventType(), event.getRecipientEmail(), ex.getMessage());
        }
    }
}
