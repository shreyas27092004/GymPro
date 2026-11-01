package com.gympro.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Received from plan-service (POST /payments/internal/subscription-refund)
 * as step 5 of the enterprise cancellation flow (Problem #5). refundAmount
 * may legitimately be 0.0 — payment-service still records the calculation
 * for audit purposes even when no money moves.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRefundRequest {
    private Long   subscriptionId;
    private Double refundAmount;
    private String reason;
}
