package com.gympro.booking.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;   // ← ADD THIS


/**
 * Mirror of plan-service's FreeSessionCheckResult DTO.
 * Feign deserializes the JSON response from plan-service into this class.
 * Kept local to booking-service to avoid a shared-library dependency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor                 // ← ADD THIS

public class FreeSessionCheckResult {

    /** true = this booking should be FREE */
    private boolean isFree;

    /**
     * Sessions remaining BEFORE consuming one.
     * -1 = unlimited. 0 = none.
     */
    private int remainingFreeSessions;

    /** Total free sessions the plan includes. -1 = unlimited. */
    private int totalFreeSessions;

    /** Subscription ID for reference. */
    private Long subscriptionId;
}
