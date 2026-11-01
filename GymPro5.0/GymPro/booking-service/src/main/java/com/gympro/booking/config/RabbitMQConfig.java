package com.gympro.booking.config;
// ─────────────────────────────────────────────────────────────────────────────
//  RabbitMQConfig – PRODUCER side (booking-service)
//
//  Copy this file into:
//    booking-service  → package com.gympro.booking.config;  (keep as-is)
//    payment-service  → package com.gympro.payment.config;  (change package only)
//    auth-service     → package com.gympro.auth.config;     (change package only)
//
//  Topology:
//    Exchange : gympro.exchange          (TopicExchange)
//    Queue    : gympro.booking.queue     → routing key "booking.notification"
//    Queue    : gympro.payment.queue     → routing key "payment.notification"
//    Queue    : gympro.otp.queue         → routing key "auth.otp"
//
//  Dead-Letter Exchange (DLX) catches failed messages after max retries:
//    DLX Exchange : gympro.dlx.exchange
//    DLX Queue    : gympro.booking.dlq / gympro.payment.dlq / gympro.otp.dlq
// ─────────────────────────────────────────────────────────────────────────────

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Exchange names ────────────────────────────────────────────────────
    public static final String EXCHANGE     = "gympro.exchange";
    public static final String DLX_EXCHANGE = "gympro.dlx.exchange";

    // ── Queue names ───────────────────────────────────────────────────────
    public static final String BOOKING_QUEUE = "gympro.booking.queue";
    public static final String PAYMENT_QUEUE = "gympro.payment.queue";
    public static final String OTP_QUEUE     = "gympro.otp.queue";

    // ── Dead-Letter Queue names ───────────────────────────────────────────
    public static final String BOOKING_DLQ   = "gympro.booking.dlq";
    public static final String PAYMENT_DLQ   = "gympro.payment.dlq";
    public static final String OTP_DLQ       = "gympro.otp.dlq";

    // ── Routing keys ──────────────────────────────────────────────────────
    public static final String BOOKING_ROUTING_KEY = "booking.notification";
    public static final String PAYMENT_ROUTING_KEY = "payment.notification";
    public static final String OTP_ROUTING_KEY     = "auth.otp";

    // ═════════════════════════════════════════════════════════════════════
    //  EXCHANGES
    // ═════════════════════════════════════════════════════════════════════

    /** Main topic exchange — all GymPro notifications route through this */
    @Bean
    public TopicExchange gymproExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE)
                .durable(true)
                .build();
    }

    /** Dead-letter exchange — receives messages that fail after max retries */
    @Bean
    public TopicExchange dlxExchange() {
        return ExchangeBuilder.topicExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  MAIN QUEUES  (with DLX configured so failed msgs go to DLQ)
    // ═════════════════════════════════════════════════════════════════════

    @Bean
    public Queue bookingQueue() {
        return QueueBuilder.durable(BOOKING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", BOOKING_DLQ)
                .build();
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_DLQ)
                .build();
    }

    @Bean
    public Queue otpQueue() {
        return QueueBuilder.durable(OTP_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OTP_DLQ)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  DEAD-LETTER QUEUES  (simple durable queues — no further DLX)
    // ═════════════════════════════════════════════════════════════════════

    @Bean
    public Queue bookingDlq() {
        return QueueBuilder.durable(BOOKING_DLQ).build();
    }

    @Bean
    public Queue paymentDlq() {
        return QueueBuilder.durable(PAYMENT_DLQ).build();
    }

    @Bean
    public Queue otpDlq() {
        return QueueBuilder.durable(OTP_DLQ).build();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  BINDINGS  (queue ↔ exchange with routing key)
    // ═════════════════════════════════════════════════════════════════════

    @Bean
    public Binding bookingBinding(Queue bookingQueue, TopicExchange gymproExchange) {
        return BindingBuilder.bind(bookingQueue)
                .to(gymproExchange)
                .with(BOOKING_ROUTING_KEY);
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange gymproExchange) {
        return BindingBuilder.bind(paymentQueue)
                .to(gymproExchange)
                .with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Binding otpBinding(Queue otpQueue, TopicExchange gymproExchange) {
        return BindingBuilder.bind(otpQueue)
                .to(gymproExchange)
                .with(OTP_ROUTING_KEY);
    }

    // DLQ → DLX bindings
    @Bean
    public Binding bookingDlqBinding(Queue bookingDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(bookingDlq)
                .to(dlxExchange)
                .with(BOOKING_DLQ);
    }

    @Bean
    public Binding paymentDlqBinding(Queue paymentDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(paymentDlq)
                .to(dlxExchange)
                .with(PAYMENT_DLQ);
    }

    @Bean
    public Binding otpDlqBinding(Queue otpDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(otpDlq)
                .to(dlxExchange)
                .with(OTP_DLQ);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  MESSAGE CONVERTER  (serialize NotificationEvent as JSON)
    // ═════════════════════════════════════════════════════════════════════

    /** Converts Java objects → JSON when publishing, JSON → Java when consuming */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Override the default RabbitTemplate to use JSON serialization.
     * Without this, Spring uses Java binary serialization — not human-readable
     * and fails across services with different classpaths.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
