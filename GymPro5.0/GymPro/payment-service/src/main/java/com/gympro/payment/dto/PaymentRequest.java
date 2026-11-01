package com.gympro.payment.dto;

import lombok.Data;

// DTO = Data Transfer Object
// What the client sends in the request body when making a payment
@Data
public class PaymentRequest {

    // Who is paying
    private Long memberId;
    private String memberEmail;

    // What are they paying for (at least one should be set)
    private Long bookingId;          // For booking a session
    private Long subscriptionId;     // For subscribing to a plan

    // Payment details
    private Double amount;           // Amount in rupees (e.g. 999.0). Must be 0.0 for FREE_SESSION.

    // Payment method:
    // CREDIT_CARD | DEBIT_CARD | UPI | QR_CODE | CASH = dummy (always succeeds)
    // RAZORPAY = real payment (needs razorpay fields below)
    // FREE     = no payment (used automatically for FREE_SESSION paymentType)
    private String paymentMethod;

    private String description;      // Human readable e.g. "Booking #5 - Ravi Trainer"

    // ── NEW: Payment type ──────────────────────────────────────────────────
    /**
     * REGULAR      = standard payment flow (default if omitted)
     * FREE_SESSION = booking covered by membership plan — no money collected
     *
     * When paymentType=FREE_SESSION:
     *   - amount must be 0.0 (or null — will be forced to 0.0)
     *   - paymentMethod is ignored (overridden to "FREE" internally)
     *   - Razorpay order is NOT created
     */
    private String paymentType;  // REGULAR | FREE_SESSION (default: REGULAR)

    // ─── Razorpay fields (only for RAZORPAY method) ────────────────────
    // After frontend completes Razorpay payment, it sends these back
    private String razorpayOrderId;   // e.g. order_ABC123 (from createOrder)
    private String razorpayPaymentId; // e.g. pay_XYZ789 (from Razorpay callback)
    private String razorpaySignature; // HMAC-SHA256 signature for verification
}
