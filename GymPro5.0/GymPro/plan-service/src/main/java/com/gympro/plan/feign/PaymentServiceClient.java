package com.gympro.plan.feign;

import com.gympro.plan.dto.SubscriptionRefundRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client – calls payment-service as step 5 of the enterprise
 * cancellation flow (Problem #5): once plan-service has calculated the
 * refund and deactivated the subscription, it notifies payment-service so
 * the original Payment record is marked REFUNDED with the correct
 * (possibly partial, possibly zero) amount.
 */
@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    // Calls POST /payments/internal/subscription-refund on payment-service
    @PostMapping("/payments/internal/subscription-refund")
    Object refundSubscription(@RequestBody SubscriptionRefundRequest request);
}
