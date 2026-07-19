package com.gympro.payment;

import com.gympro.payment.dto.PaymentRequest;
import com.gympro.payment.dto.RazorpayOrderResponse;
import com.gympro.payment.entity.Payment;
import com.gympro.payment.exception.PaymentException;
import com.gympro.payment.exception.PaymentNotFoundException;
import com.gympro.payment.service.RazorpayService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentModelsAndRazorpayTest {

    // ─── Payment entity ───────────────────────────────────────────────────

    @Test
    void testPayment_SettersAndGetters() {
        Payment p = new Payment();
        p.setId(1L);
        p.setMemberId(10L);
        p.setMemberEmail("test@example.com");
        p.setBookingId(5L);
        p.setSubscriptionId(3L);
        p.setAmount(999.0);
        p.setPaymentMethod("UPI");
        p.setStatus("SUCCESS");
        p.setTransactionId("TXN-TEST1234");
        p.setDescription("Booking payment");
        LocalDateTime now = LocalDateTime.now();
        p.setPaidAt(now);
        p.setRefundAmount(250.0);
        p.setRefundedAt(now);
        p.setRefundStatus("PENDING");
        p.setRefundReason("Trainer never showed up");
        p.setRefundAdminNote("Reviewing");
        p.setRefundRequestedAt(now);
        p.setRefundDecisionAt(now);
        p.setRefundCompletedAt(now);

        assertEquals(1L, p.getId());
        assertEquals(10L, p.getMemberId());
        assertEquals("test@example.com", p.getMemberEmail());
        assertEquals(5L, p.getBookingId());
        assertEquals(3L, p.getSubscriptionId());
        assertEquals(999.0, p.getAmount());
        assertEquals("UPI", p.getPaymentMethod());
        assertEquals("SUCCESS", p.getStatus());
        assertEquals("TXN-TEST1234", p.getTransactionId());
        assertEquals("Booking payment", p.getDescription());
        assertEquals(now, p.getPaidAt());
        assertEquals(250.0, p.getRefundAmount());
        assertEquals(now, p.getRefundedAt());
        assertEquals("PENDING", p.getRefundStatus());
        assertEquals("Trainer never showed up", p.getRefundReason());
        assertEquals("Reviewing", p.getRefundAdminNote());
        assertEquals(now, p.getRefundRequestedAt());
        assertEquals(now, p.getRefundDecisionAt());
        assertEquals(now, p.getRefundCompletedAt());
    }

    @Test
    void testPayment_AllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        // Field order: id, memberId, memberEmail, bookingId, subscriptionId, amount,
        // paymentMethod, status, transactionId, description, paidAt, paymentType,
        // refundAmount, refundedAt, refundStatus, refundReason, refundAdminNote,
        // refundRequestedAt, refundDecisionAt, refundCompletedAt
        Payment p = new Payment(1L, 2L, "user@test.com", 3L, 4L,
            500.0, "CASH", "SUCCESS", "TXN-XYZ", "desc", now, "REGULAR",
            250.0, now,
            "PENDING", "Trainer no-show", "Investigating", now, null, null);

        assertEquals(1L, p.getId());
        assertEquals(2L, p.getMemberId());
        assertEquals("user@test.com", p.getMemberEmail());
        assertEquals("CASH", p.getPaymentMethod());
        assertEquals("SUCCESS", p.getStatus());
        assertEquals(250.0, p.getRefundAmount());
        assertEquals(now, p.getRefundedAt());
        assertEquals("PENDING", p.getRefundStatus());
        assertEquals("Trainer no-show", p.getRefundReason());
        assertEquals("Investigating", p.getRefundAdminNote());
        assertEquals(now, p.getRefundRequestedAt());
    }

    @Test
    void testPayment_NoArgsConstructor() {
        Payment p = new Payment();
        assertNull(p.getId());
        assertNull(p.getMemberId());
        assertNull(p.getStatus());
    }

    @Test
    void testPayment_EqualsAndHashCode() {
        Payment p1 = new Payment();
        p1.setId(1L);
        p1.setStatus("SUCCESS");

        Payment p2 = new Payment();
        p2.setId(1L);
        p2.setStatus("SUCCESS");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void testPayment_ToString() {
        Payment p = new Payment();
        p.setId(1L);
        p.setStatus("SUCCESS");
        String str = p.toString();
        assertNotNull(str);
        assertTrue(str.contains("Payment"));
    }

    // ─── PaymentRequest DTO ───────────────────────────────────────────────

    @Test
    void testPaymentRequest_SettersAndGetters() {
        PaymentRequest req = new PaymentRequest();
        req.setMemberId(1L);
        req.setMemberEmail("user@test.com");
        req.setBookingId(2L);
        req.setSubscriptionId(3L);
        req.setAmount(750.0);
        req.setPaymentMethod("CREDIT_CARD");
        req.setDescription("Test payment");
        req.setRazorpayOrderId("order_ABC");
        req.setRazorpayPaymentId("pay_XYZ");
        req.setRazorpaySignature("sig_123");

        assertEquals(1L, req.getMemberId());
        assertEquals("user@test.com", req.getMemberEmail());
        assertEquals(2L, req.getBookingId());
        assertEquals(3L, req.getSubscriptionId());
        assertEquals(750.0, req.getAmount());
        assertEquals("CREDIT_CARD", req.getPaymentMethod());
        assertEquals("Test payment", req.getDescription());
        assertEquals("order_ABC", req.getRazorpayOrderId());
        assertEquals("pay_XYZ", req.getRazorpayPaymentId());
        assertEquals("sig_123", req.getRazorpaySignature());
    }

    // ─── RazorpayOrderResponse DTO ────────────────────────────────────────

    @Test
    void testRazorpayOrderResponse_SettersAndGetters() {
        RazorpayOrderResponse resp = new RazorpayOrderResponse(
            "order_ABC123", 999.0, "INR", "created", "rzp_test_key123"
        );

        assertEquals("order_ABC123", resp.getOrderId());
        assertEquals(999.0, resp.getAmount());
        assertEquals("INR", resp.getCurrency());
        assertEquals("created", resp.getStatus());
        assertEquals("rzp_test_key123", resp.getKeyId());
    }

    @Test
    void testRazorpayOrderResponse_SettersWork() {
        RazorpayOrderResponse resp = new RazorpayOrderResponse(
            "order_1", 100.0, "INR", "created", "key1"
        );
        resp.setOrderId("order_updated");
        resp.setAmount(200.0);
        resp.setStatus("attempted");

        assertEquals("order_updated", resp.getOrderId());
        assertEquals(200.0, resp.getAmount());
        assertEquals("attempted", resp.getStatus());
    }

    // ─── Custom exceptions ────────────────────────────────────────────────

    @Test
    void testPaymentException_MessageConstructor() {
        PaymentException ex = new PaymentException("test error");
        assertEquals("test error", ex.getMessage());
    }

    @Test
    void testPaymentException_MessageAndCauseConstructor() {
        RuntimeException cause = new RuntimeException("root cause");
        PaymentException ex = new PaymentException("wrapped error", cause);
        assertEquals("wrapped error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testPaymentNotFoundException_MessageConstructor() {
        PaymentNotFoundException ex = new PaymentNotFoundException("not found: 99");
        assertEquals("not found: 99", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void testPaymentNotFoundException_IsRuntimeException() {
        PaymentNotFoundException ex = new PaymentNotFoundException("msg");
        assertInstanceOf(RuntimeException.class, ex);
    }

    // ─── RazorpayService.verifyPayment ────────────────────────────────────

    @Test
    void testRazorpayService_VerifyPayment_ValidSignature() throws Exception {
        RazorpayService service = new RazorpayService();
        ReflectionTestUtils.setField(service, "keySecret", "test_secret");
        ReflectionTestUtils.setField(service, "keyId", "rzp_test_key");

        // Generate the correct HMAC-SHA256 signature
        String orderId = "order_ABC";
        String paymentId = "pay_XYZ";
        String data = orderId + "|" + paymentId;

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
            "test_secret".getBytes("UTF-8"), "HmacSHA256"
        );
        mac.init(keySpec);
        byte[] hashBytes = mac.doFinal(data.getBytes("UTF-8"));
        String validSignature = java.util.HexFormat.of().formatHex(hashBytes);

        boolean result = service.verifyPayment(orderId, paymentId, validSignature);

        assertTrue(result);
    }

    @Test
    void testRazorpayService_VerifyPayment_InvalidSignature() {
        RazorpayService service = new RazorpayService();
        ReflectionTestUtils.setField(service, "keySecret", "test_secret");
        ReflectionTestUtils.setField(service, "keyId", "rzp_test_key");

        boolean result = service.verifyPayment("order_ABC", "pay_XYZ", "wrong_signature");

        assertFalse(result);
    }

    @Test
    void testRazorpayService_CreateOrder_NullClient_ThrowsException() {
        RazorpayService service = new RazorpayService();
        ReflectionTestUtils.setField(service, "keyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "keySecret", "test_secret");
        // razorpayClient is null (not set)

        PaymentException ex = assertThrows(PaymentException.class, () ->
            service.createOrder(999.0, "Test order")
        );
        assertTrue(ex.getMessage().contains("not configured"));
    }
}