package com.gympro.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by PlanService.checkFreeSession() and exposed via REST.
 * Booking-service consumes this via Feign to decide if payment is required.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreeSessionCheckResult {

    /** true  = this booking should be FREE (member has sessions remaining)
     *  false = payment is required */
    private boolean isFree;

    /**
     * How many free sessions remain BEFORE this booking.
     * -1 = unlimited (plan has sessionsIncluded = -1).
     *  0 = none remaining.
     */
    private int remainingFreeSessions;

    /**
     * Total free sessions included in the plan (for display purposes).
     * -1 = unlimited.
     */
    private int totalFreeSessions;

    /** The subscription ID that was evaluated (useful for debugging). */
    private Long subscriptionId;
}
