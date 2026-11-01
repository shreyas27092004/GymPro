package com.gympro.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// ✅ One payment record per transaction
// Can be for a booking session OR a plan subscription
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who is paying
    private Long memberId;
    private String memberEmail;

    // What are they paying for – one of these will be set
    private Long bookingId;       // if paying for a session booking (nullable)
    private Long subscriptionId;  // if paying for a plan subscription (nullable)

    // Payment details
    private Double amount;

    // CREDIT_CARD | DEBIT_CARD | UPI | QR_CODE | CASH | RAZORPAY
    private String paymentMethod;

    // PENDING | SUCCESS | FAILED | REFUNDED | FREE
    private String status;

    // Dummy transaction reference generated internally
    private String transactionId;

    // Readable description e.g. "Booking #5 – Ravi Trainer MON 09:00"
    private String description;

    private LocalDateTime paidAt;

    // ── NEW: Payment type ─────────────────────────────────────────────────
    /**
     * REGULAR       = standard payment (Razorpay, cash, UPI, etc.)
     * FREE_SESSION  = no money collected — covered by membership plan
     *
     * When paymentType=FREE_SESSION:
     *   - amount must be 0.0
     *   - paymentMethod = "FREE"
     *   - status = "FREE"
     *   - no Razorpay order is created
     */
    @Column(name = "payment_type", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'REGULAR'")
    private String paymentType;  // REGULAR | FREE_SESSION

    // ── NEW: Refund tracking (Problem #4) ─────────────────────────────────
    /**
     * The amount actually refunded. May be LESS than `amount` for a
     * prorated subscription-cancellation refund, or exactly 0.0 if the
     * member used more than 80% of the plan (no-refund policy).
     * Null until a refund has been calculated/processed for this payment.
     */
    @Column(name = "refund_amount")
    private Double refundAmount;

    /** Timestamp the refund was processed (null until refunded). */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    // ── NEW: Refund workflow ────────────────────────────────────────────────
    /**
     * Tracks the member-initiated refund request lifecycle, independent of
     * the overall payment `status`. Null until a refund is first requested.
     *
     * PENDING    → member submitted a refund request, awaiting admin review
     * APPROVED   → admin approved the request (transient — moves to PROCESSING immediately)
     * REJECTED   → admin rejected the request (member may request again)
     * PROCESSING → refund is being executed (transient — moves to COMPLETED immediately)
     * COMPLETED  → refund finished; payment.status is set to REFUNDED
     */
    @Column(name = "refund_status", length = 20)
    private String refundStatus;

    // Reason the member gave when requesting the refund
    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    // Optional note the admin gave when approving/rejecting
    @Column(name = "refund_admin_note", length = 500)
    private String refundAdminNote;

    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundDecisionAt;
    private LocalDateTime refundCompletedAt;
}
