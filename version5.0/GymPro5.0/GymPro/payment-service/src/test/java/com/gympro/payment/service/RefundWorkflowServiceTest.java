package com.gympro.payment.service;

import com.gympro.payment.dto.NotificationEvent;
import com.gympro.payment.dto.RefundRequestDto;
import com.gympro.payment.entity.Payment;
import com.gympro.payment.entity.RefundStatus;
import com.gympro.payment.exception.PaymentException;
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
 * Unit tests for the member-request → admin-approval refund workflow:
 *   requestRefund() / approveRefund() / rejectRefund() / getPendingRefunds()
 */
@ExtendWith(MockitoExtension.class)
class RefundWorkflowServiceTest {

    @Mock private PaymentRepository repo;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private RazorpayService razorpayService;

    @InjectMocks
    private PaymentService paymentService;

    private Payment successPayment;

    @BeforeEach
    void setUp() {
        successPayment = new Payment();
        successPayment.setId(1L);
        successPayment.setMemberId(10L);
        successPayment.setMemberEmail("member@gmail.com");
        successPayment.setAmount(999.0);
        successPayment.setStatus("SUCCESS");
        successPayment.setPaymentType("REGULAR");
        successPayment.setTransactionId("TXN-ABC123");
    }

    // ─── requestRefund ────────────────────────────────────────────────────

    @Test
    void testRequestRefund_Success_SetsPendingAndNotifiesAdmins() {
        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));
        when(repo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundRequestDto req = new RefundRequestDto();
        req.setMemberId(10L);
        req.setReason("Trainer never showed up");

        Payment result = paymentService.requestRefund(1L, req);

        assertEquals(RefundStatus.PENDING, result.getRefundStatus());
        assertEquals("Trainer never showed up", result.getRefundReason());
        assertNotNull(result.getRefundRequestedAt());
        verify(notificationPublisher, times(1)).publishPaymentEvent(any(NotificationEvent.class));
    }

    @Test
    void testRequestRefund_MissingReason_ThrowsException() {
        RefundRequestDto req = new RefundRequestDto();
        req.setMemberId(10L);
        req.setReason("  ");

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.requestRefund(1L, req)
        );
        assertTrue(ex.getMessage().contains("reason"));
        verify(repo, never()).findById(anyLong());
    }

    @Test
    void testRequestRefund_MissingMemberId_ThrowsException() {
        RefundRequestDto req = new RefundRequestDto();
        req.setReason("Changed my mind");

        assertThrows(PaymentException.class, () -> paymentService.requestRefund(1L, req));
    }

    @Test
    void testRequestRefund_WrongMember_ThrowsSecurityException() {
        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));

        RefundRequestDto req = new RefundRequestDto();
        req.setMemberId(99L); // not the payment's memberId (10L)
        req.setReason("Not mine but trying anyway");

        assertThrows(SecurityException.class, () -> paymentService.requestRefund(1L, req));
    }

    @Test
    void testRequestRefund_FreeSession_ThrowsException() {
        successPayment.setPaymentType("FREE_SESSION");
        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));

        RefundRequestDto req = new RefundRequestDto();
        req.setMemberId(10L);
        req.setReason("Want money back for free session");

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.requestRefund(1L, req)
        );
        assertTrue(ex.getMessage().contains("FREE_SESSION"));
    }

    @Test
    void testRequestRefund_NonSuccessStatus_ThrowsException() {
        successPayment.setStatus("PENDING");
        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));

        RefundRequestDto req = new RefundRequestDto();
        req.setMemberId(10L);
        req.setReason("Too slow");

        assertThrows(PaymentException.class, () -> paymentService.requestRefund(1L, req));
    }

    @Test
    void testRequestRefund_AlreadyPending_ThrowsException() {
        successPayment.setRefundStatus(RefundStatus.PENDING);
        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));

        RefundRequestDto req = new RefundRequestDto();
        req.setMemberId(10L);
        req.setReason("Asking again");

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.requestRefund(1L, req)
        );
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void testRequestRefund_AfterRejection_AllowsResubmission() {
        successPayment.setRefundStatus(RefundStatus.REJECTED);
        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));
        when(repo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundRequestDto req = new RefundRequestDto();
        req.setMemberId(10L);
        req.setReason("Trying again with more detail");

        Payment result = paymentService.requestRefund(1L, req);

        assertEquals(RefundStatus.PENDING, result.getRefundStatus());
    }

    // ─── approveRefund ────────────────────────────────────────────────────

    @Test
    void testApproveRefund_Success_CompletesAndRefunds() {
        successPayment.setRefundStatus(RefundStatus.PENDING);
        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));
        when(repo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.approveRefund(1L, "ADMIN", "Approved — valid complaint");

        assertEquals("REFUNDED", result.getStatus());
        assertEquals(RefundStatus.COMPLETED, result.getRefundStatus());
        assertNotNull(result.getRefundCompletedAt());
        // Two notifications: REFUND_APPROVED then REFUND_COMPLETED
        verify(notificationPublisher, times(2)).publishPaymentEvent(any(NotificationEvent.class));
    }

    @Test
    void testApproveRefund_NonAdmin_ThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
            paymentService.approveRefund(1L, "MEMBER", null)
        );
        verify(repo, never()).findById(anyLong());
    }

    @Test
    void testApproveRefund_NotPending_ThrowsException() {
        successPayment.setRefundStatus(null); // no refund ever requested
        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.approveRefund(1L, "ADMIN", null)
        );
        assertTrue(ex.getMessage().contains("No pending refund request"));
    }

    // ─── rejectRefund ─────────────────────────────────────────────────────

    @Test
    void testRejectRefund_Success_SetsRejectedAndKeepsStatus() {
        successPayment.setRefundStatus(RefundStatus.PENDING);
        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));
        when(repo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.rejectRefund(1L, "ADMIN", "No evidence of the issue");

        assertEquals(RefundStatus.REJECTED, result.getRefundStatus());
        assertEquals("SUCCESS", result.getStatus()); // untouched — no money moved
        assertEquals("No evidence of the issue", result.getRefundAdminNote());
        verify(notificationPublisher, times(1)).publishPaymentEvent(any(NotificationEvent.class));
    }

    @Test
    void testRejectRefund_MissingReason_ThrowsException() {
        successPayment.setRefundStatus(RefundStatus.PENDING);

        PaymentException ex = assertThrows(PaymentException.class, () ->
            paymentService.rejectRefund(1L, "ADMIN", "  ")
        );
        assertTrue(ex.getMessage().contains("reason"));
        verify(repo, never()).findById(anyLong());
    }

    @Test
    void testRejectRefund_NonAdmin_ThrowsSecurityException() {
        assertThrows(SecurityException.class, () ->
            paymentService.rejectRefund(1L, "MEMBER", "Some reason")
        );
    }

    @Test
    void testRejectRefund_NotPending_ThrowsException() {
        successPayment.setRefundStatus(RefundStatus.APPROVED);
        when(repo.findById(1L)).thenReturn(Optional.of(successPayment));

        assertThrows(PaymentException.class, () ->
            paymentService.rejectRefund(1L, "ADMIN", "Too late")
        );
    }

    // ─── getPendingRefunds ────────────────────────────────────────────────

    @Test
    void testGetPendingRefunds_ReturnsOnlyPending() {
        Payment p1 = new Payment(); p1.setId(1L); p1.setRefundStatus(RefundStatus.PENDING);
        when(repo.findByRefundStatus(RefundStatus.PENDING)).thenReturn(List.of(p1));

        List<Payment> result = paymentService.getPendingRefunds();

        assertEquals(1, result.size());
        verify(repo, times(1)).findByRefundStatus(RefundStatus.PENDING);
    }
}
