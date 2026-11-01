package com.gympro.payment.service;

import com.gympro.payment.dto.NotificationEvent;
import com.gympro.payment.dto.PaymentRequest;
import com.gympro.payment.dto.PlanPrivileges;
import com.gympro.payment.dto.RazorpayOrderResponse;
import com.gympro.payment.dto.RefundRequestDto;
import com.gympro.payment.entity.Payment;
import com.gympro.payment.entity.RefundStatus;
import com.gympro.payment.exception.PaymentException;
import com.gympro.payment.exception.PaymentNotFoundException;
import com.gympro.payment.messaging.NotificationPublisher;
import com.gympro.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
//  PaymentService — enhanced to support FREE_SESSION payment type
//
//  Three payment modes:
//    1. FREE_SESSION → No money collected. paymentType=FREE_SESSION in request.
//    2. RAZORPAY     → Real payment via Razorpay.
//    3. DUMMY        → Simulated (UPI/CASH/QR).
//
//  Existing Razorpay and dummy flows are untouched.
// ─────────────────────────────────────────────────────────────────────────────
@Slf4j
@Service
public class PaymentService {

    @Autowired private PaymentRepository     repo;
    @Autowired private NotificationPublisher notificationPublisher;
    @Autowired private RazorpayService       razorpayService;
    @Autowired private com.gympro.payment.feign.AuthServiceClient authServiceClient;
    @Autowired private com.gympro.payment.feign.BookingServiceClient bookingServiceClient;
    @Autowired private com.gympro.payment.feign.PlanServiceClient planServiceClient;

    /**
     * Best-effort fetch of every ADMIN-role user ID, for broadcasting admin
     * in-app notifications. Never throws — must not block the member's
     * payment/refund notification flow if auth-service is briefly unreachable.
     */
    private java.util.List<Long> fetchAdminUserIds() {
        try {
            return authServiceClient.getAdminUserIds();
        } catch (Exception e) {
            log.warn("⚠️ Could not fetch admin user IDs for broadcast notification: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    // NOTE: "FREE" is intentionally excluded here. It must only ever be
    // reachable through the paymentType=FREE_SESSION short-circuit in
    // processPayment() → processFreeSessionPayment(). Allowing "FREE" as a
    // regular paymentMethod let a REGULAR-type payment slip through with
    // status=SUCCESS (counted as revenue, displayed as "Paid" on the admin
    // dashboard) even though no money was actually collected.
    private static final List<String> VALID_METHODS =
        List.of("CREDIT_CARD", "DEBIT_CARD", "UPI", "QR_CODE", "CASH", "RAZORPAY");

    // ════════════════════════════════════════════════════════════════════════
    //  STEP 1: Create Razorpay Order (unchanged — not used for FREE_SESSION)
    // ════════════════════════════════════════════════════════════════════════

    public RazorpayOrderResponse createRazorpayOrder(Double amount, String description) {
        if (amount == null || amount <= 0) {
            throw new PaymentException("Amount must be greater than 0");
        }
        return razorpayService.createOrder(amount, description);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STEP 2: Process Payment
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Handles three flows:
     *   - FREE_SESSION: records a zero-amount FREE payment record (no gateway).
     *   - RAZORPAY:     verifies signature, records payment.
     *   - DUMMY:        simulates payment for UPI/CASH/QR.
     */
    public Payment processPayment(PaymentRequest req) {
        // ── FREE_SESSION short-circuit ───────────────────────────────────
        if ("FREE_SESSION".equalsIgnoreCase(req.getPaymentType())) {
            return processFreeSessionPayment(req);
        }

        // ── Standard flow ────────────────────────────────────────────────
        validatePaymentRequest(req);
        applyTrainerDiscountIfApplicable(req);
        Payment payment = buildPayment(req);

        if ("RAZORPAY".equalsIgnoreCase(req.getPaymentMethod())) {
            payment = processRazorpayPayment(payment, req);
        } else {
            payment = processDummyPayment(payment, req);
        }

        Payment saved = repo.save(payment);
        log.info("✅ Payment saved: id={}, txn={}, status={}", saved.getId(), saved.getTransactionId(), saved.getStatus());

        // Sync booking-service so the booking's paymentStatus reflects this
        // payment's outcome (COMPLETED on SUCCESS). Without this, the booking
        // stays PENDING forever even though the payment itself succeeded.
        if ("SUCCESS".equals(saved.getStatus()) && saved.getBookingId() != null) {
            confirmBookingPaymentAsync(saved.getBookingId());
        }

        sendPaymentReceiptAsync(saved);
        return saved;
    }

    /**
     * Best-effort call to booking-service to mark the booking's paymentStatus
     * as COMPLETED. Never throws — a transient booking-service outage must not
     * fail the already-successful payment, but is logged loudly so it's visible.
     */
    private void confirmBookingPaymentAsync(Long bookingId) {
        try {
            bookingServiceClient.confirmPayment(bookingId);
            log.info("✅ Booking #{} paymentStatus synced to COMPLETED", bookingId);
        } catch (Exception e) {
            log.error("⚠️ Failed to sync paymentStatus=COMPLETED to booking-service for bookingId={}: {}",
                bookingId, e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GETTERS  (unchanged)
    // ════════════════════════════════════════════════════════════════════════

    public List<Payment> getByMember(Long memberId) {
        return repo.findByMemberId(memberId);
    }

    public Payment getById(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
    }

    public Payment getByTransactionId(String txnId) {
        return repo.findByTransactionId(txnId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found with txnId: " + txnId));
    }

    public Payment getByBookingId(Long bookingId) {
        return repo.findByBookingId(bookingId)
            .orElseThrow(() -> new PaymentNotFoundException("No payment found for booking: " + bookingId));
    }

    public List<Payment> getAll() {
        return repo.findAll();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  REFUND  (direct, admin-initiated refund — FREE_SESSION payments cannot be refunded)
    // ════════════════════════════════════════════════════════════════════════

    public Payment refund(Long id, String role) {
        if (!"ADMIN".equals(role)) {
            throw new SecurityException("Access denied – only ADMIN can issue refunds");
        }

        Payment payment = getById(id);

        if ("FREE_SESSION".equals(payment.getPaymentType())) {
            throw new PaymentException("Cannot refund a FREE_SESSION payment — no money was collected.");
        }

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new PaymentException(
                "Cannot refund payment with status: " + payment.getStatus() +
                ". Only SUCCESS payments can be refunded."
            );
        }

        payment.setStatus("REFUNDED");
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundedAt(LocalDateTime.now());
        // Keep the refund-workflow fields consistent even for this direct,
        // admin-initiated refund path (no member request was involved).
        payment.setRefundStatus(RefundStatus.COMPLETED);
        payment.setRefundCompletedAt(LocalDateTime.now());
        Payment saved = repo.save(payment);
        log.info("✅ Refund processed for payment: {}", id);

        try {
            NotificationEvent refundEvent = NotificationEvent.paymentRefund(
                payment.getMemberEmail(),
                payment.getMemberId(),
                payment.getAmount(),
                payment.getTransactionId()
            );
            refundEvent.setPaymentId(payment.getId());
            refundEvent.setAdminUserIds(fetchAdminUserIds());
            notificationPublisher.publishPaymentEvent(refundEvent);
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error while publishing refund notification: {}", e.getMessage());
        }

        return saved;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TRAINER SESSION DISCOUNT  (Problem #7 — auto-applied, data-driven)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * For a REGULAR payment tied to a booking (trainer session), looks up the
     * member's active plan privileges and discounts req.amount by
     * trainerDiscountPercent — automatically, with zero hardcoded plan names.
     * Best-effort: if plan-service is unreachable, the payment proceeds at
     * full price rather than failing (fail-open, matches the pattern used
     * for admin-broadcast notification lookups elsewhere in this class).
     * No-op for subscription payments (bookingId == null) — a plan
     * subscription is never discounted by itself.
     */
    private void applyTrainerDiscountIfApplicable(PaymentRequest req) {
        if (req.getBookingId() == null || req.getMemberId() == null) return;
        if (req.getAmount() == null || req.getAmount() <= 0) return;

        try {
            PlanPrivileges privileges = planServiceClient.getPrivileges(req.getMemberId());
            if (privileges == null || !privileges.isHasActivePlan()) return;

            double discountPercent = privileges.getTrainerDiscountPercent();
            if (discountPercent <= 0) return;

            double discounted = req.getAmount() * (1 - (discountPercent / 100.0));
            discounted = Math.round(discounted * 100.0) / 100.0;

            log.info("✅ Trainer session discount applied: memberId={}, plan={}, {}% off ₹{} -> ₹{}",
                req.getMemberId(), privileges.getPlanName(), discountPercent, req.getAmount(), discounted);

            req.setAmount(discounted);
            String note = "(" + discountPercent + "% " + privileges.getPlanName() + " plan discount applied)";
            req.setDescription(req.getDescription() == null ? note : req.getDescription() + " " + note);
        } catch (Exception e) {
            log.warn("⚠️ Could not fetch plan privileges for discount lookup, charging full price: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SUBSCRIPTION CANCELLATION REFUND  (Problems #4, #5 — called by plan-service)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Called by plan-service (via Feign, POST /payments/internal/subscription-refund)
     * once it has calculated the prorated cancellation refund and deactivated
     * the subscription. Finds the original payment for that subscription and
     * records the refund — which may be a partial amount or exactly 0.0
     * (member used >80% of the plan), per the enterprise cancellation policy.
     * Always records refundAmount/refundedAt for audit even when 0.0; only
     * flips status to REFUNDED when money actually moves.
     */
    public Payment refundForSubscriptionCancellation(Long subscriptionId, Double refundAmount, String reason) {
        Payment payment = repo.findBySubscriptionId(subscriptionId)
            .orElseThrow(() -> new PaymentNotFoundException("No payment found for subscription: " + subscriptionId));

        double amount = refundAmount != null ? refundAmount : 0.0;

        if ("FREE_SESSION".equals(payment.getPaymentType())) {
            // Nothing was ever collected — nothing to refund, but still record the calculation.
            payment.setRefundAmount(0.0);
            payment.setRefundedAt(LocalDateTime.now());
            return repo.save(payment);
        }

        payment.setRefundAmount(amount);
        payment.setRefundedAt(LocalDateTime.now());
        if (amount > 0) {
            payment.setStatus("REFUNDED");
            payment.setRefundStatus(RefundStatus.COMPLETED);
            payment.setRefundCompletedAt(LocalDateTime.now());
        }
        Payment saved = repo.save(payment);
        log.info("✅ Subscription cancellation refund recorded: paymentId={}, subscriptionId={}, refund=₹{}",
            saved.getId(), subscriptionId, amount);

        if (amount > 0) {
            try {
                NotificationEvent refundEvent = NotificationEvent.paymentRefund(
                    payment.getMemberEmail(), payment.getMemberId(), amount, payment.getTransactionId()
                );
                refundEvent.setPaymentId(payment.getId());
                refundEvent.setAdminUserIds(fetchAdminUserIds());
                notificationPublisher.publishPaymentEvent(refundEvent);
            } catch (Exception e) {
                log.warn("⚠️ Unexpected error while publishing subscription-cancellation refund notification: {}", e.getMessage());
            }
        }

        return saved;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  REFUND WORKFLOW (member request → admin approval → processing)
    //
    //   Member ──► Refund Request ──► Payment Service ──► Admin Notification
    //                                                          │
    //                                                    Pending Approval
    //                                                          │
    //                                              ┌───────────┴───────────┐
    //                                        Admin Approves          Admin Rejects
    //                                              │                       │
    //                                       Refund Process          Member Notified
    //                                              │                  (REJECTED)
    //                                       Member Notified
    //                                     (APPROVED + COMPLETED)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Step 1: MEMBER submits a refund request for one of their own SUCCESS
     * payments. Never touches money — just opens a PENDING request and
     * notifies every admin.
     */
    public Payment requestRefund(Long id, RefundRequestDto req) {
        if (req == null || req.getMemberId() == null) {
            throw new PaymentException("memberId is required");
        }
        if (req.getReason() == null || req.getReason().isBlank()) {
            throw new PaymentException("A reason is required to request a refund");
        }

        Payment payment = getById(id);

        if (!req.getMemberId().equals(payment.getMemberId())) {
            throw new SecurityException("Access denied – this payment does not belong to the requesting member");
        }
        if ("FREE_SESSION".equals(payment.getPaymentType())) {
            throw new PaymentException("Cannot request a refund for a FREE_SESSION payment — no money was collected.");
        }
        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new PaymentException(
                "Cannot request a refund for payment with status: " + payment.getStatus() +
                ". Only SUCCESS payments are eligible."
            );
        }
        // Allow re-requesting after a REJECTED decision, but block duplicate
        // requests while one is already PENDING/APPROVED/PROCESSING/COMPLETED.
        String existing = payment.getRefundStatus();
        if (existing != null && !RefundStatus.REJECTED.equals(existing)) {
            throw new PaymentException("A refund request already exists for this payment (status: " + existing + ")");
        }

        payment.setRefundStatus(RefundStatus.PENDING);
        payment.setRefundReason(req.getReason().trim());
        payment.setRefundAdminNote(null);
        payment.setRefundRequestedAt(LocalDateTime.now());
        payment.setRefundDecisionAt(null);
        payment.setRefundCompletedAt(null);

        Payment saved = repo.save(payment);
        log.info("✅ Refund requested: paymentId={}, memberId={}", id, req.getMemberId());

        try {
            NotificationEvent event = NotificationEvent.refundRequested(
                payment.getMemberEmail(),
                payment.getAmount(),
                payment.getTransactionId(),
                payment.getId(),
                payment.getRefundReason()
            );
            event.setAdminUserIds(fetchAdminUserIds());
            notificationPublisher.publishPaymentEvent(event);
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error while publishing refund-request notification: {}", e.getMessage());
        }

        return saved;
    }

    /**
     * Step 2a: ADMIN approves a PENDING refund request. In this system there
     * is no external payment gateway to wait on, so APPROVED moves straight
     * through PROCESSING to COMPLETED in the same call, and the member is
     * notified for both stages.
     */
    public Payment approveRefund(Long id, String role, String adminNote) {
        requireAdmin(role);
        Payment payment = getById(id);
        requirePendingRefund(payment);

        payment.setRefundStatus(RefundStatus.APPROVED);
        payment.setRefundAdminNote(adminNote);
        payment.setRefundDecisionAt(LocalDateTime.now());
        repo.save(payment);

        try {
            NotificationEvent approvedEvent = NotificationEvent.refundApproved(
                payment.getMemberEmail(), payment.getMemberId(),
                payment.getAmount(), payment.getTransactionId(), payment.getId()
            );
            notificationPublisher.publishPaymentEvent(approvedEvent);
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error while publishing refund-approved notification: {}", e.getMessage());
        }

        // ── Refund Process ───────────────────────────────────────────────
        payment.setRefundStatus(RefundStatus.PROCESSING);
        repo.save(payment);
        log.info("⏳ Refund processing: paymentId={}", id);

        payment.setStatus("REFUNDED");
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundedAt(LocalDateTime.now());
        payment.setRefundStatus(RefundStatus.COMPLETED);
        payment.setRefundCompletedAt(LocalDateTime.now());
        Payment saved = repo.save(payment);
        log.info("✅ Refund completed: paymentId={}", id);

        try {
            NotificationEvent completedEvent = NotificationEvent.refundCompleted(
                payment.getMemberEmail(), payment.getMemberId(),
                payment.getAmount(), payment.getTransactionId(), payment.getId()
            );
            completedEvent.setAdminUserIds(fetchAdminUserIds());
            notificationPublisher.publishPaymentEvent(completedEvent);
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error while publishing refund-completed notification: {}", e.getMessage());
        }

        return saved;
    }

    /**
     * Step 2b: ADMIN rejects a PENDING refund request. Money is untouched —
     * payment.status stays SUCCESS. Member may submit a new request later.
     */
    public Payment rejectRefund(Long id, String role, String adminNote) {
        requireAdmin(role);
        if (adminNote == null || adminNote.isBlank()) {
            throw new PaymentException("A reason is required to reject a refund request");
        }

        Payment payment = getById(id);
        requirePendingRefund(payment);

        payment.setRefundStatus(RefundStatus.REJECTED);
        payment.setRefundAdminNote(adminNote.trim());
        payment.setRefundDecisionAt(LocalDateTime.now());
        Payment saved = repo.save(payment);
        log.info("✅ Refund rejected: paymentId={}", id);

        try {
            NotificationEvent rejectedEvent = NotificationEvent.refundRejected(
                payment.getMemberEmail(), payment.getMemberId(),
                payment.getAmount(), payment.getTransactionId(), payment.getId(), payment.getRefundAdminNote()
            );
            notificationPublisher.publishPaymentEvent(rejectedEvent);
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error while publishing refund-rejected notification: {}", e.getMessage());
        }

        return saved;
    }

    /** Admin queue: every payment with a refund request awaiting a decision. */
    public List<Payment> getPendingRefunds() {
        return repo.findByRefundStatus(RefundStatus.PENDING);
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new SecurityException("Access denied – only ADMIN can review refund requests");
        }
    }

    private void requirePendingRefund(Payment payment) {
        if (!RefundStatus.PENDING.equals(payment.getRefundStatus())) {
            throw new PaymentException(
                "No pending refund request for this payment (refundStatus: " + payment.getRefundStatus() + ")"
            );
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Records a FREE_SESSION payment — amount=0, no gateway interaction.
     * Persists a clean audit record for the member's payment history.
     */
    private Payment processFreeSessionPayment(PaymentRequest req) {
        if (req.getMemberId() == null) throw new PaymentException("memberId is required");
        if (req.getMemberEmail() == null || req.getMemberEmail().isBlank())
            throw new PaymentException("memberEmail is required");

        Payment p = new Payment();
        p.setMemberId(req.getMemberId());
        p.setMemberEmail(req.getMemberEmail());
        p.setBookingId(req.getBookingId());
        p.setSubscriptionId(req.getSubscriptionId());
        p.setAmount(0.0);
        p.setPaymentMethod("FREE");
        p.setPaymentType("FREE_SESSION");
        p.setDescription(req.getDescription() != null ? req.getDescription() : "Free session — covered by membership plan");
        p.setStatus("FREE");
        p.setTransactionId("FREE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setPaidAt(LocalDateTime.now());

        Payment saved = repo.save(p);
        log.info("✅ FREE_SESSION payment recorded: id={}, txn={}, bookingId={}", saved.getId(), saved.getTransactionId(), saved.getBookingId());
        return saved;
    }

    private Payment processRazorpayPayment(Payment payment, PaymentRequest req) {
        if (req.getRazorpayOrderId() == null || req.getRazorpayPaymentId() == null || req.getRazorpaySignature() == null) {
            throw new PaymentException(
                "For RAZORPAY method, provide: razorpayOrderId, razorpayPaymentId, razorpaySignature"
            );
        }

        boolean isValid = razorpayService.verifyPayment(
            req.getRazorpayOrderId(),
            req.getRazorpayPaymentId(),
            req.getRazorpaySignature()
        );

        if (!isValid) {
            payment.setStatus("FAILED");
            payment.setTransactionId("FAILED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            repo.save(payment);
            throw new PaymentException("Payment verification failed! Invalid signature.");
        }

        payment.setTransactionId(req.getRazorpayPaymentId());
        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        log.info("✅ Razorpay payment verified: {}", req.getRazorpayPaymentId());
        return payment;
    }

    private Payment processDummyPayment(Payment payment, PaymentRequest req) {
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payment.setTransactionId(txnId);
        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        log.info("✅ Dummy payment processed: txn={}", txnId);
        return payment;
    }

    private Payment buildPayment(PaymentRequest req) {
        Payment p = new Payment();
        p.setMemberId(req.getMemberId());
        p.setMemberEmail(req.getMemberEmail());
        p.setBookingId(req.getBookingId());
        p.setSubscriptionId(req.getSubscriptionId());
        p.setAmount(req.getAmount());
        p.setPaymentMethod(req.getPaymentMethod().toUpperCase());
        p.setPaymentType("REGULAR");
        p.setDescription(req.getDescription());
        p.setStatus("PENDING");
        return p;
    }

    private void validatePaymentRequest(PaymentRequest req) {
        if (req.getMemberId() == null) {
            throw new PaymentException("memberId is required");
        }
        if (req.getMemberEmail() == null || req.getMemberEmail().isBlank()) {
            throw new PaymentException("memberEmail is required");
        }
        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new PaymentException("amount must be greater than 0");
        }
        if (req.getPaymentMethod() == null || req.getPaymentMethod().isBlank()) {
            throw new PaymentException("paymentMethod is required");
        }
        if (!VALID_METHODS.contains(req.getPaymentMethod().toUpperCase())) {
            throw new PaymentException(
                "Invalid paymentMethod: " + req.getPaymentMethod() +
                ". Valid options: " + VALID_METHODS
            );
        }
    }

    private void sendPaymentReceiptAsync(Payment payment) {
        try {
            NotificationEvent successEvent = NotificationEvent.paymentSuccess(
                payment.getMemberEmail(),
                payment.getMemberId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getTransactionId(),
                payment.getDescription() != null ? payment.getDescription() : "GymPro Payment"
            );
            successEvent.setAdminUserIds(fetchAdminUserIds());
            notificationPublisher.publishPaymentEvent(successEvent);
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error while publishing payment receipt notification: {}", e.getMessage());
        }
    }
}
