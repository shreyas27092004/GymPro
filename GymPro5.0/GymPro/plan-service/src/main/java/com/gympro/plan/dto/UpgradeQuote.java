package com.gympro.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by PlanService.getUpgradeQuote() / GET /plans/upgrade-quote.
 * Read-only preview of what an upgrade would cost — does NOT mutate any
 * subscription. The frontend uses this to show the member the amount to
 * pay before actually confirming the upgrade via POST /plans/upgrade.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeQuote {

    private Long   currentSubscriptionId;
    private Long   currentPlanId;
    private String currentPlanName;

    private Long   newPlanId;
    private String newPlanName;
    private double newPlanPrice;

    /** Prorated remaining value of the current plan (unused days / total days * price). */
    private double remainingValue;

    /** newPlanPrice - remainingValue, floored at 0. What the member must pay now. */
    private double amountToPay;
}
