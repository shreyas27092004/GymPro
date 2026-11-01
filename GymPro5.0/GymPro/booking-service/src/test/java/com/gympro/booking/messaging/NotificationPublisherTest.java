package com.gympro.booking.messaging;

import com.gympro.booking.config.RabbitMQConfig;
import com.gympro.booking.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link NotificationPublisher}.
 * The {@link RabbitTemplate} is mocked — no real RabbitMQ broker is used.
 */
@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private NotificationPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new NotificationPublisher();
        ReflectionTestUtils.setField(publisher, "rabbitTemplate", rabbitTemplate);
    }

    @Test
    @DisplayName("publishBookingEvent sends to the booking routing key")
    void publishBookingEvent_sendsWithBookingRoutingKey() {
        NotificationEvent event = NotificationEvent.bookingConfirmed("m@x.com", 1L, 10L, "Coach", "MON", "09:00");

        publisher.publishBookingEvent(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.BOOKING_ROUTING_KEY), eq(event));
    }

    @Test
    @DisplayName("publishPaymentEvent sends to the payment routing key")
    void publishPaymentEvent_sendsWithPaymentRoutingKey() {
        NotificationEvent event = NotificationEvent.paymentSuccess("m@x.com", 1L, 100.0, "UPI", "t1", "d");

        publisher.publishPaymentEvent(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.PAYMENT_ROUTING_KEY), eq(event));
    }

    @Test
    @DisplayName("publishOtpEvent sends to the OTP routing key")
    void publishOtpEvent_sendsWithOtpRoutingKey() {
        NotificationEvent event = NotificationEvent.otpRequested("m@x.com", "123456");

        publisher.publishOtpEvent(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.OTP_ROUTING_KEY), eq(event));
    }

    @Test
    @DisplayName("Swallows AmqpException so a broker outage never propagates to the caller")
    void publish_amqpExceptionThrown_isSwallowed() {
        NotificationEvent event = NotificationEvent.bookingCancelled("m@x.com", 1L, 10L);
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertDoesNotThrow(() -> publisher.publishBookingEvent(event));
    }
}
