package com.gympro.payment.service;

import com.gympro.payment.dto.NotificationEvent;
import com.gympro.payment.dto.PaymentRequest;
import com.gympro.payment.dto.RazorpayOrderResponse;
import com.gympro.payment.entity.Payment;
import com.gympro.payment.exception.PaymentException;
import com.gympro.payment.exception.PaymentNotFoundException;
import com.gympro.payment.messaging.NotificationPublisher;
import com.gympro.payment.repository.PaymentRepository;
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
 * Extended unit tests for PaymentService.
 * PaymentService now uses NotificationPublisher (RabbitMQ async events)
 * instead of the old synchronous Feign NotificationClient.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceExtendedTest {

    @Mock private PaymentRepository repo;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private RazorpayService razorpayService;

    @InjectMocks private PaymentService paymentService;

    private PaymentRequest validUpiRequest;

    @BeforeEach
    void setUp() {
        validUpiRequest = new PaymentRequest();
        validUpiRequest.setMemberId(1L);
        validUpiRequest.setMemberEmail("user@gmail.com");
        validUpiRequest.setAmount(500.0);
        validUpiRequest.setPaymentMethod("UPI");
        validUpiRequest.setDescription("Test booking payment");
        validUpiRequest.setBookingId(10L);
    }

    // ─── Validation: null email ──────────────────────────────────────────

    @Test
    void testProcessPayment_NullEmail_ThrowsException() {
        validUpiRequest.setMemberEmail(null);
        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validUpiRequest)
        );
        assertTrue(ex.getMessage().contains("memberEmail is required"));
    }

    @Test
    void testProcessPayment_BlankEmail_ThrowsException() {
        validUpiRequest.setMemberEmail("   ");
        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validUpiRequest)
        );
        assertTrue(ex.getMessage().contains("memberEmail is required"));
    }

    // ─── Validation: null amount ─────────────────────────────────────────

    @Test
    void testProcessPayment_NullAmount_ThrowsException() {
        validUpiRequest.setAmount(null);
        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validUpiRequest)
        );
        assertTrue(ex.getMessage().contains("amount must be greater than 0"));
    }

    @Test
    void testProcessPayment_NegativeAmount_ThrowsException() {
        validUpiRequest.setAmount(-100.0);
        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validUpiRequest)
        );
        assertTrue(ex.getMessage().contains("amount must be greater than 0"));
    }

    // ─── Validation: null/blank paymentMethod ────────────────────────────

    @Test
    void testProcessPayment_NullPaymentMethod_ThrowsException() {
        validUpiRequest.setPaymentMethod(null);
        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validUpiRequest)
        );
        assertTrue(ex.getMessage().contains("paymentMethod is required"));
    }

    @Test
    void testProcessPayment_BlankPaymentMethod_ThrowsException() {
        validUpiRequest.setPaymentMethod("   ");
        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validUpiRequest)
        );
        assertTrue(ex.getMessage().contains("paymentMethod is required"));
    }

    // ─── FREE payment type ───────────────────────────────────────────────

    @Test
    void testProcessPayment_FreePaymentType_Success() {
        validUpiRequest.setPaymentMethod("FREE");
        validUpiRequest.setPaymentType("FREE_SESSION");
        mockSaveReturnsWithId();
        doNothing().when(notificationPublisher).publishPaymentEvent(any(NotificationEvent.class));

        Payment result = paymentService.processPayment(validUpiRequest);

        assertNotNull(result);
        // Regression guard for the "Free sessions show as Paid" admin-dashboard
        // bug: a FREE_SESSION payment must be recorded with status=FREE (not
        // SUCCESS), amount=0, so it's correctly excluded from revenue and
        // never rendered as "Paid".
        assertEquals("FREE", result.getStatus());
        assertEquals("FREE_SESSION", result.getPaymentType());
        assertEquals(0.0, result.getAmount());
        assertEquals("FREE", result.getPaymentMethod());
    }

    @Test
    void testProcessPayment_FreeMethodWithoutFreeSessionType_ThrowsException() {
        // Regression guard: "FREE" must never be a valid method for the
        // REGULAR flow — otherwise a payment could end up SUCCESS/counted
        // as revenue without any money actually being collected.
        validUpiRequest.setPaymentMethod("FREE");
        validUpiRequest.setPaymentType("REGULAR");

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validUpiRequest)
        );
        assertTrue(ex.getMessage().contains("Invalid paymentMethod"));
        verify(repo, never()).save(any(Payment.class));
    }

    // ─── Dummy payment methods ───────────────────────────────────────────

    @Test
    void testProcessPayment_QR_CODE_Success() {
        validUpiRequest.setPaymentMethod("QR_CODE");
        mockSaveReturnsWithId();
        doNothing().when(notificationPublisher).publishPaymentEvent(any(NotificationEvent.class));

        Payment result = paymentService.processPayment(validUpiRequest);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("QR_CODE", result.getPaymentMethod());
    }

    @Test
    void testProcessPayment_CASH_Success() {
        validUpiRequest.setPaymentMethod("CASH");
        mockSaveReturnsWithId();
        doNothing().when(notificationPublisher).publishPaymentEvent(any(NotificationEvent.class));

        Payment result = paymentService.processPayment(validUpiRequest);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("CASH", result.getPaymentMethod());
    }

    @Test
    void testProcessPayment_CREDIT_CARD_Success() {
        validUpiRequest.setPaymentMethod("CREDIT_CARD");
        mockSaveReturnsWithId();
        doNothing().when(notificationPublisher).publishPaymentEvent(any(NotificationEvent.class));

        Payment result = paymentService.processPayment(validUpiRequest);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("CREDIT_CARD", result.getPaymentMethod());
    }

    @Test
    void testProcessPayment_DEBIT_CARD_Success() {
        validUpiRequest.setPaymentMethod("DEBIT_CARD");
        mockSaveReturnsWithId();
        doNothing().when(notificationPublisher).publishPaymentEvent(any(NotificationEvent.class));

        Payment result = paymentService.processPayment(validUpiRequest);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("DEBIT_CARD", result.getPaymentMethod());
    }

    // ─── RAZORPAY path ────────────────────────────────────────────────────

    @Test
    void testProcessPayment_Razorpay_MissingFields_ThrowsException() {
        validUpiRequest.setPaymentMethod("RAZORPAY");

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validUpiRequest)
        );
        assertTrue(ex.getMessage().contains("razorpayOrderId"));
    }

    @Test
    void testProcessPayment_Razorpay_InvalidSignature_SavesFailedAndThrows() {
        validUpiRequest.setPaymentMethod("RAZORPAY");
        validUpiRequest.setRazorpayOrderId("order_ABC");
        validUpiRequest.setRazorpayPaymentId("pay_XYZ");
        validUpiRequest.setRazorpaySignature("bad_sig");

        when(razorpayService.verifyPayment("order_ABC", "pay_XYZ", "bad_sig")).thenReturn(false);
        when(repo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.processPayment(validUpiRequest)
        );
        assertTrue(ex.getMessage().contains("verification failed"));
        verify(repo, atLeastOnce()).save(any(Payment.class));
    }

    @Test
    void testProcessPayment_Razorpay_ValidSignature_Success() {
        validUpiRequest.setPaymentMethod("RAZORPAY");
        validUpiRequest.setRazorpayOrderId("order_ABC");
        validUpiRequest.setRazorpayPaymentId("pay_XYZ789");
        validUpiRequest.setRazorpaySignature("valid_sig");

        when(razorpayService.verifyPayment("order_ABC", "pay_XYZ789", "valid_sig")).thenReturn(true);
        mockSaveReturnsWithId();
        doNothing().when(notificationPublisher).publishPaymentEvent(any(NotificationEvent.class));

        Payment result = paymentService.processPayment(validUpiRequest);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("pay_XYZ789", result.getTransactionId());
    }

    // ─── createRazorpayOrder ──────────────────────────────────────────────

    @Test
    void testCreateRazorpayOrder_NullAmount_ThrowsException() {
        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.createRazorpayOrder(null, "Test")
        );
        assertTrue(ex.getMessage().contains("Amount must be greater than 0"));
    }

    @Test
    void testCreateRazorpayOrder_ZeroAmount_ThrowsException() {
        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.createRazorpayOrder(0.0, "Test")
        );
        assertTrue(ex.getMessage().contains("Amount must be greater than 0"));
    }

    @Test
    void testCreateRazorpayOrder_ValidAmount_Success() {
        RazorpayOrderResponse mockResp = new RazorpayOrderResponse(
            "order_ABC", 999.0, "INR", "created", "rzp_test_key"
        );
        when(razorpayService.createOrder(999.0, "Booking")).thenReturn(mockResp);

        RazorpayOrderResponse result = paymentService.createRazorpayOrder(999.0, "Booking");

        assertNotNull(result);
        assertEquals("order_ABC", result.getOrderId());
    }

    // ─── getByTransactionId ───────────────────────────────────────────────

    @Test
    void testGetByTransactionId_Found() {
        Payment p = new Payment(); p.setTransactionId("TXN-XYZ");
        when(repo.findByTransactionId("TXN-XYZ")).thenReturn(Optional.of(p));

        Payment result = paymentService.getByTransactionId("TXN-XYZ");

        assertEquals("TXN-XYZ", result.getTransactionId());
    }

    @Test
    void testGetByTransactionId_NotFound_ThrowsException() {
        when(repo.findByTransactionId("TXN-MISSING")).thenReturn(Optional.empty());

        PaymentNotFoundException ex = assertThrows(PaymentNotFoundException.class, () ->
            paymentService.getByTransactionId("TXN-MISSING")
        );
        assertTrue(ex.getMessage().contains("TXN-MISSING"));
    }

    // ─── getByBookingId ───────────────────────────────────────────────────

    @Test
    void testGetByBookingId_Found() {
        Payment p = new Payment(); p.setBookingId(7L);
        when(repo.findByBookingId(7L)).thenReturn(Optional.of(p));

        Payment result = paymentService.getByBookingId(7L);

        assertEquals(7L, result.getBookingId());
    }

    @Test
    void testGetByBookingId_NotFound_ThrowsException() {
        when(repo.findByBookingId(99L)).thenReturn(Optional.empty());

        PaymentNotFoundException ex = assertThrows(PaymentNotFoundException.class, () ->
            paymentService.getByBookingId(99L)
        );
        assertTrue(ex.getMessage().contains("booking: 99"));
    }

    // ─── getByMember ──────────────────────────────────────────────────────

    @Test
    void testGetByMember_ReturnsList() {
        Payment p1 = new Payment(); p1.setMemberId(5L);
        Payment p2 = new Payment(); p2.setMemberId(5L);
        when(repo.findByMemberId(5L)).thenReturn(List.of(p1, p2));

        List<Payment> result = paymentService.getByMember(5L);

        assertEquals(2, result.size());
    }

    // ─── refund / notification failure is swallowed ───────────────────────

    @Test
    void testRefund_NotificationFails_StillReturnsRefunded() {
        Payment p = new Payment();
        p.setId(1L);
        p.setStatus("SUCCESS");
        p.setAmount(500.0);
        p.setMemberEmail("member@gmail.com");
        p.setTransactionId("TXN-REF123");

        when(repo.findById(1L)).thenReturn(Optional.of(p));
        when(repo.save(any(Payment.class))).thenReturn(p);
        doThrow(new RuntimeException("RabbitMQ down"))
            .when(notificationPublisher).publishPaymentEvent(any(NotificationEvent.class));

        Payment result = paymentService.refund(1L, "ADMIN");

        assertEquals("REFUNDED", result.getStatus());
    }

    @Test
    void testProcessPayment_NotificationFails_PaymentStillSaved() {
        mockSaveReturnsWithId();
        doThrow(new RuntimeException("RabbitMQ down"))
            .when(notificationPublisher).publishPaymentEvent(any(NotificationEvent.class));

        assertDoesNotThrow(() -> paymentService.processPayment(validUpiRequest));
        verify(repo, times(1)).save(any(Payment.class));
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    private void mockSaveReturnsWithId() {
        when(repo.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
    }
}
