package com.gympro.payment.entity;

/**
 * Refund workflow status constants — see {@link Payment#getRefundStatus()}.
 *
 * Flow:
 *   (null) → PENDING → APPROVED → PROCESSING → COMPLETED
 *                    → REJECTED  (member may request again → back to PENDING)
 */
public final class RefundStatus {

    public static final String PENDING    = "PENDING";
    public static final String APPROVED   = "APPROVED";
    public static final String REJECTED   = "REJECTED";
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED  = "COMPLETED";

    private RefundStatus() {}
}
