package com.gympro.payment.feign;

import com.gympro.payment.dto.PlanPrivileges;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for plan-service.
 * Used by PaymentService to look up the member's active plan privileges so
 * the configured trainer-session discount (Problem #7) can be applied
 * automatically to booking-session payments, without hardcoding any
 * plan-name -> discount mapping here.
 */
@FeignClient(name = "plan-service")
public interface PlanServiceClient {

    /** GET /plans/privileges/{memberId} — returns the member's active plan privileges. */
    @GetMapping("/plans/privileges/{memberId}")
    PlanPrivileges getPrivileges(@PathVariable("memberId") Long memberId);
}
