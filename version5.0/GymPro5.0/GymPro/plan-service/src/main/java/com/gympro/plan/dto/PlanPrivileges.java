package com.gympro.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by GET /plans/privileges/{memberId}.
 *
 * Exposes the *configured* privileges of a member's active plan so other
 * services (payment-service for trainer-session discounts, booking/trainer
 * services for priority booking or dedicated-trainer access) can make
 * decisions WITHOUT hardcoding plan names — everything comes from the
 * MembershipPlan row an admin configured (Problem #6, #7).
 *
 * If the member has no active plan, all privilege fields are "no privilege"
 * defaults (priorityLevel=0, discount=0, dedicatedTrainer/priorityBooking=false).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanPrivileges {

    private boolean hasActivePlan;
    private Long    planId;
    private String  planName;

    private int     priorityLevel;
    private double  trainerDiscountPercent;
    private boolean dedicatedTrainer;
    private boolean priorityBooking;

    public static PlanPrivileges none() {
        return new PlanPrivileges(false, null, null, 0, 0.0, false, false);
    }
}
