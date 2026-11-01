package com.gympro.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sent via Feign to payment-service (POST /payments/internal/subscription-refund)
 * as step 5 of the enterprise cancellation flow (Problem #5):
 *   Cancel Request -> Validate -> Calculate refund -> Deactivate plan
 *   -> Notify payment service -> Notify member.
 *
 * refundAmount may legitimately be 0.0 (e.g. member used >80% of the plan);
 * payment-service still records the calculation for audit purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRefundRequest {
    private Long   subscriptionId;
    private Double refundAmount;
    private String reason;
}
