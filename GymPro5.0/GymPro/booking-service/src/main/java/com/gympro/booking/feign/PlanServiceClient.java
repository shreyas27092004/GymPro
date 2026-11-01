package com.gympro.booking.feign;

import com.gympro.booking.dto.FreeSessionCheckResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for plan-service.
 *
 * Used by BookingService to:
 *   1. checkFreeSession     — read-only eligibility check (no side effect)
 *   2. useIncludedSession   — atomically consume one free session
 *   3. restoreFreeSession   — restore one free session on booking cancellation
 *   4. linkFreeSessionToBooking — update audit bookingId on usage record
 */
@FeignClient(name = "plan-service")
public interface PlanServiceClient {

    /**
     * GET /plans/free-session-check/{memberId}
     * Returns eligibility without consuming a session.
     * Handles both subscribed and non-subscribed members.
     */
    @GetMapping("/plans/free-session-check/{memberId}")
    FreeSessionCheckResult checkFreeSession(@PathVariable("memberId") Long memberId);

    /**
     * POST /plans/use-session/{memberId}
     * Atomically consumes one free session.
     * Returns true if successful, false if none remain.
     */
    @PostMapping("/plans/use-session/{memberId}")
    boolean useIncludedSession(@PathVariable("memberId") Long memberId);

    /**
     * POST /plans/restore-session/{memberId}
     * Restores one free session (called on cancellation of FREE_SESSION bookings).
     */
    @PostMapping("/plans/restore-session/{memberId}")
    String restoreFreeSession(@PathVariable("memberId") Long memberId);
}
