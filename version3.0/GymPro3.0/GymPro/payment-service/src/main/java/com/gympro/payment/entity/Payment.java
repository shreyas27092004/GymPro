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
}
