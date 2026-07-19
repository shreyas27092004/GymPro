package com.gympro.payment;

import com.gympro.payment.dto.PaymentRequest;
import com.gympro.payment.dto.PlanPrivileges;
import com.gympro.payment.entity.Payment;
import com.gympro.payment.exception.PaymentNotFoundException;
import com.gympro.payment.feign.AuthServiceClient;
import com.gympro.payment.feign.BookingServiceClient;
import com.gympro.payment.feign.PlanServiceClient;
import com.gympro.payment.messaging.NotificationPublisher;
import com.gympro.payment.repository.PaymentRepository;
import com.gympro.payment.service.PaymentService;
import com.gympro.payment.service.RazorpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Covers the new Problem #4/#5 (subscription cancellation refund) and
 * Problem #7 (auto-applied trainer session discount) logic in PaymentService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService — trainer discount / subscription cancellation refund")
class PaymentServiceDiscountRefundTest {

    @Mock private PaymentRepository     repo;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private RazorpayService       razorpayService;
    @Mock private AuthServiceClient     authServiceClient;
    @Mock private BookingServiceClient  bookingServiceClient;
    @Mock private PlanServiceClient     planServiceClient;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(repo.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
        lenient().when(authServiceClient.getAdminUserIds()).thenReturn(java.util.List.of());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Problem #7 — trainer session discount auto-apply
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Trainer session discount")
    class Discount {

        @Test
        @DisplayName("Applies the member's configured plan discount to a booking payment")
        void appliesConfiguredDiscount() {
            PaymentRequest req = new PaymentRequest();
            req.setMemberId(1L);
            req.setMemberEmail("member@gym.com");
            req.setAmount(1000.0);
            req.setPaymentMethod("UPI");
            req.setBookingId(5L);

            when(planServiceClient.getPrivileges(1L))
                .thenReturn(new PlanPrivileges(true, 3L, "Premium", 3, 25.0, true, false));

            Payment saved = paymentService.processPayment(req);

            // 25% off ₹1000 = ₹750
            assertEquals(750.0, saved.getAmount());
        }

        @Test
        @DisplayName("No discount applied when member has no active plan")
        void noDiscountWithoutActivePlan() {
            PaymentRequest req = new PaymentRequest();
            req.setMemberId(1L);
            req.setMemberEmail("member@gym.com");
            req.setAmount(1000.0);
            req.setPaymentMethod("UPI");
            req.setBookingId(5L);

            when(planServiceClient.getPrivileges(1L)).thenReturn(noPrivileges());

            Payment saved = paymentService.processPayment(req);

            assertEquals(1000.0, saved.getAmount());
        }

        @Test
        @DisplayName("Subscription payments (no bookingId) are never discounted")
        void subscriptionPaymentsNeverDiscounted() {
            PaymentRequest req = new PaymentRequest();
            req.setMemberId(1L);
            req.setMemberEmail("member@gym.com");
            req.setAmount(2000.0);
            req.setPaymentMethod("UPI");
            req.setSubscriptionId(9L); // no bookingId set

            Payment saved = paymentService.processPayment(req);

            assertEquals(2000.0, saved.getAmount());
            verifyNoInteractions(planServiceClient);
        }

        @Test
        @DisplayName("Payment still succeeds at full price if plan-service is unreachable")
        void failsOpenWhenPlanServiceDown() {
            PaymentRequest req = new PaymentRequest();
            req.setMemberId(1L);
            req.setMemberEmail("member@gym.com");
            req.setAmount(1000.0);
            req.setPaymentMethod("UPI");
            req.setBookingId(5L);

            when(planServiceClient.getPrivileges(1L)).thenThrow(new RuntimeException("plan-service down"));

            Payment saved = paymentService.processPayment(req);

            assertEquals(1000.0, saved.getAmount());
            assertEquals("SUCCESS", saved.getStatus());
        }

        private PlanPrivileges noPrivileges() {
            return new PlanPrivileges(false, null, null, 0, 0.0, false, false);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Problem #4 / #5 — subscription cancellation refund
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("refundForSubscriptionCancellation()")
    class SubscriptionRefund {

        @Test
        @DisplayName("Records a partial refund and marks payment REFUNDED when amount > 0")
        void recordsPartialRefund() {
            Payment original = new Payment();
            original.setId(50L);
            original.setSubscriptionId(9L);
            original.setAmount(1000.0);
            original.setStatus("SUCCESS");
            original.setPaymentType("REGULAR");
            original.setMemberEmail("member@gym.com");
            original.setMemberId(1L);
            original.setTransactionId("TXN-ABC");

            when(repo.findBySubscriptionId(9L)).thenReturn(Optional.of(original));

            Payment result = paymentService.refundForSubscriptionCancellation(9L, 733.33, "Plan cancellation");

            assertEquals("REFUNDED", result.getStatus());
            assertEquals(733.33, result.getRefundAmount());
            assertNotNull(result.getRefundedAt());
        }

        @Test
        @DisplayName("Records a 0.0 refund without changing status when usage exceeds 80%")
        void recordsZeroRefundWithoutStatusChange() {
            Payment original = new Payment();
            original.setId(50L);
            original.setSubscriptionId(9L);
            original.setAmount(1000.0);
            original.setStatus("SUCCESS");
            original.setPaymentType("REGULAR");

            when(repo.findBySubscriptionId(9L)).thenReturn(Optional.of(original));

            Payment result = paymentService.refundForSubscriptionCancellation(9L, 0.0, "Plan cancellation");

            assertEquals("SUCCESS", result.getStatus()); // unchanged — no money moved
            assertEquals(0.0, result.getRefundAmount());
        }

        @Test
        @DisplayName("Throws PaymentNotFoundException when no payment exists for the subscription")
        void throwsWhenPaymentMissing() {
            when(repo.findBySubscriptionId(999L)).thenReturn(Optional.empty());

            assertThrows(PaymentNotFoundException.class,
                () -> paymentService.refundForSubscriptionCancellation(999L, 500.0, "reason"));
        }

        @Test
        @DisplayName("FREE_SESSION payments record a 0.0 refund with no status change")
        void freeSessionPaymentsRecordZeroRefund() {
            Payment original = new Payment();
            original.setId(50L);
            original.setSubscriptionId(9L);
            original.setAmount(0.0);
            original.setStatus("FREE");
            original.setPaymentType("FREE_SESSION");

            when(repo.findBySubscriptionId(9L)).thenReturn(Optional.of(original));

            Payment result = paymentService.refundForSubscriptionCancellation(9L, 500.0, "reason");

            assertEquals("FREE", result.getStatus());
            assertEquals(0.0, result.getRefundAmount());
        }
    }
}
