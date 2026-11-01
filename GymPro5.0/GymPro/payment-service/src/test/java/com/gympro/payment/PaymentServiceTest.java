package com.gympro.payment;

import com.gympro.payment.dto.NotificationEvent;
import com.gympro.payment.dto.PaymentRequest;
import com.gympro.payment.entity.Payment;
import com.gympro.payment.exception.PaymentException;
import com.gympro.payment.exception.PaymentNotFoundException;
import com.gympro.payment.messaging.NotificationPublisher;
import com.gympro.payment.repository.PaymentRepository;
import com.gympro.payment.service.PaymentService;
import com.gympro.payment.service.RazorpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 * Uses NotificationPublisher (RabbitMQ) — replaces old Feign NotificationClient.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository repo;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private RazorpayService razorpayService;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new PaymentRequest();
        validRequest.setMemberId(1L);
        validRequest.setMemberEmail("shreyas@gmail.com");
        validRequest.setAmount(999.0);
        validRequest.setPaymentMethod("UPI");
        validRequest.setDescription("Test payment");
        validRequest.setBookingId(1L);
    }

    @Test
    void testProcessPayment_UPI_Success() {
        when(repo.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
        doNothing().when(notificationPublisher).publishPaymentEvent(any(NotificationEvent.class));

        Payment result = paymentService.processPayment(validRequest);

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("UPI", result.getPaymentMethod());
        assertEquals(999.0, result.getAmount());
        assertNotNull(result.getTransactionId());
        assertTrue(result.getTransactionId().startsWith("TXN-"));
        verify(repo, times(1)).save(any(Payment.class));
    }

    @Test
    void testProcessPayment_InvalidMethod_ThrowsException() {
        validRequest.setPaymentMethod("BITCOIN");

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validRequest)
        );

        assertTrue(ex.getMessage().contains("Invalid paymentMethod"));
        verify(repo, never()).save(any(Payment.class));
    }

    @Test
    void testProcessPayment_ZeroAmount_ThrowsException() {
        validRequest.setAmount(0.0);

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validRequest)
        );

        assertTrue(ex.getMessage().contains("amount must be greater than 0"));
    }

    @Test
    void testProcessPayment_NullMemberId_ThrowsException() {
        validRequest.setMemberId(null);

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validRequest)
        );

        assertTrue(ex.getMessage().contains("memberId is required"));
    }

    @Test
    void testGetById_NotFound_ThrowsException() {
        when(repo.findById(anyLong())).thenReturn(Optional.empty());

        PaymentNotFoundException ex = assertThrows(PaymentNotFoundException.class, () ->
            paymentService.getById(999L)
        );

        assertTrue(ex.getMessage().contains("Payment not found with id: 999"));
    }

    @Test
    void testGetById_Success() {
        Payment fakePayment = new Payment();
        fakePayment.setId(1L);
        fakePayment.setStatus("SUCCESS");
        fakePayment.setAmount(999.0);

        when(repo.findById(1L)).thenReturn(Optional.of(fakePayment));

        Payment result = paymentService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("SUCCESS", result.getStatus());
    }

    @Test
    void testRefund_NonAdmin_ThrowsSecurityException() {
        SecurityException ex = assertThrows(SecurityException.class, () ->
            paymentService.refund(1L, "MEMBER")
        );

        assertTrue(ex.getMessage().contains("only ADMIN"));
    }

    @Test
    void testRefund_NonSuccessPayment_ThrowsException() {
        Payment refundedPayment = new Payment();
        refundedPayment.setId(1L);
        refundedPayment.setStatus("REFUNDED");

        when(repo.findById(1L)).thenReturn(Optional.of(refundedPayment));

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.refund(1L, "ADMIN")
        );

        assertTrue(ex.getMessage().contains("Cannot refund payment with status: REFUNDED"));
    }

    @Test
    void testRefund_Admin_Success() {
        Payment successPayment = new Payment();
        successPayment.setId(1L);
        successPayment.setStatus("SUCCESS");
        successPayment.setAmount(999.0);
        successPayment.setMemberEmail("shreyas@gmail.com");
        successPayment.setTransactionId("TXN-ABC123");

        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));
        when(repo.save(any(Payment.class))).thenReturn(successPayment);
        doNothing().when(notificationPublisher).publishPaymentEvent(any(NotificationEvent.class));

        Payment result = paymentService.refund(1L, "ADMIN");

        assertEquals("REFUNDED", result.getStatus());
    }

    @Test
    void testGetAll_ReturnsAllPayments() {
        Payment p1 = new Payment(); p1.setId(1L);
        Payment p2 = new Payment(); p2.setId(2L);

        when(repo.findAll()).thenReturn(List.of(p1, p2));

        List<Payment> result = paymentService.getAll();

        assertEquals(2, result.size());
        verify(repo, times(1)).findAll();
    }
}
