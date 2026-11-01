package com.gympro.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors plan-service's PlanPrivileges DTO (GET /plans/privileges/{memberId}).
 * payment-service only needs trainerDiscountPercent (Problem #7 — the
 * discount must apply automatically during payment), but the full shape is
 * kept in sync for forward compatibility.
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
}
