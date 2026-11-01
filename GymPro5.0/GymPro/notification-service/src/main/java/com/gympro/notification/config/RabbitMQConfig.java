package com.gympro.notification.config;

// ─────────────────────────────────────────────────────────────────────────────
//  RabbitMQConfig – CONSUMER side (notification-service)
//
//  This is the CONSUMER mirror of the producer RabbitMQConfig.
//  Both sides must declare the same exchange, queues, and bindings
//  so that RabbitMQ idempotently reconciles them on startup.
//
//  Additional consumer config:
//    - SimpleRabbitListenerContainerFactory: sets concurrency, prefetch,
//      and JSON message converter for @RabbitListener methods
//    - Dead-letter exchange + DLQ bindings for failed messages
//    - RetryOperationsInterceptor: 3 retries with 2s backoff before DLQ
// ─────────────────────────────────────────────────────────────────────────────

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

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

    @Bean
    public TopicExchange gymproExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange dlxExchange() {
        return ExchangeBuilder.topicExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  MAIN QUEUES  (with DLX arguments — mirror of producer config)
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
    //  DEAD-LETTER QUEUES
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
    //  BINDINGS
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
    //  MESSAGE CONVERTER  (JSON — must match producer's converter)
    // ═════════════════════════════════════════════════════════════════════

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  RETRY INTERCEPTOR
    //  3 retries with 2-second backoff. After 3 failures the message is
    //  rejected (NOT requeued) → dead-letter exchange picks it up → DLQ.
    // ═════════════════════════════════════════════════════════════════════

    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(2000, 2.0, 10000)   // initial 2s, multiplier 2x, max 10s
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  LISTENER CONTAINER FACTORY
    //  Used by @RabbitListener(containerFactory = "rabbitListenerContainerFactory")
    //  • concurrentConsumers=1 / maxConcurrentConsumers=3 : auto-scales
    //  • prefetchCount=1 : fair dispatch — don't pile up unacked msgs
    //  • advice chain: retryInterceptor for automatic retry + DLQ routing
    // ═════════════════════════════════════════════════════════════════════

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            RetryOperationsInterceptor retryInterceptor) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);
        factory.setPrefetchCount(1);
        factory.setAdviceChain(retryInterceptor);       // enable retry + DLQ
        factory.setDefaultRequeueRejected(false);       // don't re-queue poison messages
        return factory;
    }
}
