package com.gympro.payment.service;

import com.gympro.payment.dto.NotificationEvent;
import com.gympro.payment.dto.PaymentRequest;
import com.gympro.payment.dto.RazorpayOrderResponse;
import com.gympro.payment.entity.Payment;
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

    private static final List<String> VALID_METHODS =
        List.of("CREDIT_CARD", "DEBIT_CARD", "UPI", "QR_CODE", "CASH", "RAZORPAY", "FREE");

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
        Payment payment = buildPayment(req);

        if ("RAZORPAY".equalsIgnoreCase(req.getPaymentMethod())) {
            payment = processRazorpayPayment(payment, req);
        } else {
            payment = processDummyPayment(payment, req);
        }

        Payment saved = repo.save(payment);
        log.info("✅ Payment saved: id={}, txn={}, status={}", saved.getId(), saved.getTransactionId(), saved.getStatus());

        sendPaymentReceiptAsync(saved);
        return saved;
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
    //  REFUND  (unchanged — FREE_SESSION payments cannot be refunded)
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
        Payment saved = repo.save(payment);
        log.info("✅ Refund processed for payment: {}", id);

        try {
            NotificationEvent refundEvent = NotificationEvent.paymentRefund(
                payment.getMemberEmail(),
                payment.getMemberId(),
                payment.getAmount(),
                payment.getTransactionId()
            );
            refundEvent.setAdminUserIds(fetchAdminUserIds());
            notificationPublisher.publishPaymentEvent(refundEvent);
        } catch (Exception e) {
            log.warn("⚠️ Unexpected error while publishing refund notification: {}", e.getMessage());
        }

        return saved;
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
