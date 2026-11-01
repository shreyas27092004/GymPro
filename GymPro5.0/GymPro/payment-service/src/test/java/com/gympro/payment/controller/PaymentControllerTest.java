package com.gympro.payment.controller;

import com.gympro.payment.dto.PaymentRequest;
import com.gympro.payment.dto.RazorpayOrderResponse;
import com.gympro.payment.entity.Payment;
import com.gympro.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService service;

    @InjectMocks
    private PaymentController controller;

    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        samplePayment = new Payment();
        samplePayment.setId(1L);
        samplePayment.setMemberId(10L);
        samplePayment.setMemberEmail("member@gmail.com");
        samplePayment.setAmount(999.0);
        samplePayment.setStatus("SUCCESS");
        samplePayment.setTransactionId("TXN-ABCD1234");
    }

    // ─── createOrder ──────────────────────────────────────────────────────

    @Test
    void testCreateOrder_Success() {
        RazorpayOrderResponse mockResponse = new RazorpayOrderResponse(
            "order_ABC123", 999.0, "INR", "created", "rzp_test_key"
        );
        when(service.createRazorpayOrder(999.0, "Booking #5")).thenReturn(mockResponse);

        ResponseEntity<RazorpayOrderResponse> response = controller.createOrder(999.0, "Booking #5");

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("order_ABC123", response.getBody().getOrderId());
        assertEquals(999.0, response.getBody().getAmount());
    }

    @Test
    void testCreateOrder_DefaultDescription() {
        RazorpayOrderResponse mockResponse = new RazorpayOrderResponse(
            "order_XYZ", 500.0, "INR", "created", "rzp_test_key"
        );
        when(service.createRazorpayOrder(500.0, "GymPro Payment")).thenReturn(mockResponse);

        ResponseEntity<RazorpayOrderResponse> response = controller.createOrder(500.0, "GymPro Payment");

        assertEquals(200, response.getStatusCodeValue());
        verify(service, times(1)).createRazorpayOrder(500.0, "GymPro Payment");
    }

    // ─── pay ──────────────────────────────────────────────────────────────

    @Test
    void testPay_Success() {
        PaymentRequest req = new PaymentRequest();
        req.setMemberId(10L);
        req.setMemberEmail("member@gmail.com");
        req.setAmount(999.0);
        req.setPaymentMethod("UPI");

        when(service.processPayment(req)).thenReturn(samplePayment);

        ResponseEntity<Payment> response = controller.pay(req);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getStatus());
        verify(service, times(1)).processPayment(req);
    }

    // ─── getMyPayments ────────────────────────────────────────────────────

    @Test
    void testGetMyPayments_ReturnsList() {
        Payment p2 = new Payment(); p2.setId(2L); p2.setMemberId(10L);
        when(service.getByMember(10L)).thenReturn(List.of(samplePayment, p2));

        ResponseEntity<List<Payment>> response = controller.getMyPayments(10L);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void testGetMyPayments_EmptyList() {
        when(service.getByMember(99L)).thenReturn(List.of());

        ResponseEntity<List<Payment>> response = controller.getMyPayments(99L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isEmpty());
    }

    // ─── getById ──────────────────────────────────────────────────────────

    @Test
    void testGetById_Success() {
        when(service.getById(1L)).thenReturn(samplePayment);

        ResponseEntity<Payment> response = controller.getById(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1L, response.getBody().getId());
    }

    // ─── getByTxn ─────────────────────────────────────────────────────────

    @Test
    void testGetByTxn_Success() {
        when(service.getByTransactionId("TXN-ABCD1234")).thenReturn(samplePayment);

        ResponseEntity<Payment> response = controller.getByTxn("TXN-ABCD1234");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("TXN-ABCD1234", response.getBody().getTransactionId());
    }

    // ─── getByBooking ─────────────────────────────────────────────────────

    @Test
    void testGetByBooking_Success() {
        samplePayment.setBookingId(5L);
        when(service.getByBookingId(5L)).thenReturn(samplePayment);

        ResponseEntity<Payment> response = controller.getByBooking(5L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(5L, response.getBody().getBookingId());
    }

    // ─── getAll ───────────────────────────────────────────────────────────

    @Test
    void testGetAll_AsAdmin_Success() {
        when(service.getAll()).thenReturn(List.of(samplePayment));

        ResponseEntity<List<Payment>> response = controller.getAll("ADMIN");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetAll_AsMember_ThrowsSecurityException() {
        assertThrows(SecurityException.class, () -> controller.getAll("MEMBER"));
        verify(service, never()).getAll();
    }

    // ─── refund ───────────────────────────────────────────────────────────

    @Test
    void testRefund_AsAdmin_Success() {
        samplePayment.setStatus("REFUNDED");
        when(service.refund(1L, "ADMIN")).thenReturn(samplePayment);

        ResponseEntity<Payment> response = controller.refund(1L, "ADMIN");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("REFUNDED", response.getBody().getStatus());
    }

    // ─── test endpoint ────────────────────────────────────────────────────

    @Test
    void testHealthEndpoint() {
        String result = controller.test();
        assertNotNull(result);
        assertTrue(result.contains("Payment Service Working"));
    }

    // ─── refund workflow ──────────────────────────────────────────────────

    @Test
    void testRequestRefund_Success() {
        Payment pending = new Payment();
        pending.setId(1L);
        pending.setRefundStatus("PENDING");

        com.gympro.payment.dto.RefundRequestDto dto = new com.gympro.payment.dto.RefundRequestDto();
        dto.setMemberId(10L);
        dto.setReason("Trainer cancelled");

        when(service.requestRefund(1L, dto)).thenReturn(pending);

        ResponseEntity<Payment> response = controller.requestRefund(1L, dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("PENDING", response.getBody().getRefundStatus());
        verify(service, times(1)).requestRefund(1L, dto);
    }

    @Test
    void testApproveRefund_Success() {
        Payment completed = new Payment();
        completed.setId(1L);
        completed.setStatus("REFUNDED");
        completed.setRefundStatus("COMPLETED");

        when(service.approveRefund(1L, "ADMIN", null)).thenReturn(completed);

        ResponseEntity<Payment> response = controller.approveRefund(1L, "ADMIN", null);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("COMPLETED", response.getBody().getRefundStatus());
        assertEquals("REFUNDED", response.getBody().getStatus());
    }

    @Test
    void testApproveRefund_WithNote() {
        com.gympro.payment.dto.RefundDecisionDto body = new com.gympro.payment.dto.RefundDecisionDto();
        body.setNote("Confirmed with trainer");
        Payment completed = new Payment();
        completed.setId(1L);
        completed.setRefundStatus("COMPLETED");

        when(service.approveRefund(1L, "ADMIN", "Confirmed with trainer")).thenReturn(completed);

        ResponseEntity<Payment> response = controller.approveRefund(1L, "ADMIN", body);

        assertEquals(200, response.getStatusCodeValue());
        verify(service, times(1)).approveRefund(1L, "ADMIN", "Confirmed with trainer");
    }

    @Test
    void testRejectRefund_Success() {
        com.gympro.payment.dto.RefundDecisionDto body = new com.gympro.payment.dto.RefundDecisionDto();
        body.setNote("No evidence provided");

        Payment rejected = new Payment();
        rejected.setId(1L);
        rejected.setRefundStatus("REJECTED");

        when(service.rejectRefund(1L, "ADMIN", "No evidence provided")).thenReturn(rejected);

        ResponseEntity<Payment> response = controller.rejectRefund(1L, "ADMIN", body);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("REJECTED", response.getBody().getRefundStatus());
    }

    @Test
    void testGetPendingRefunds_AsAdmin_Success() {
        Payment p = new Payment(); p.setId(1L); p.setRefundStatus("PENDING");
        when(service.getPendingRefunds()).thenReturn(List.of(p));

        ResponseEntity<List<Payment>> response = controller.getPendingRefunds("ADMIN");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetPendingRefunds_AsMember_ThrowsSecurityException() {
        assertThrows(SecurityException.class, () -> controller.getPendingRefunds("MEMBER"));
        verify(service, never()).getPendingRefunds();
    }
}
