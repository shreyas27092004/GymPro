package com.gympro.plan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "membership_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String planName;
    private String description;
    private String durationType;    // MONTHLY | QUARTERLY | YEARLY

    private double price;
    private int durationDays;       // 30, 90, 180, 365
    private boolean active;

    /**
     * How many free trainer sessions are included in this plan.
     *   0  = no free sessions (payment always required)
     *  >0  = that many free sessions included
     *  -1  = unlimited free sessions (always free)
     *
     * Column name kept as sessions_included for backward compatibility.
     * Frontend label: "Free Trainer Sessions".
     */
    @Column(name = "sessions_included", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int sessionsIncluded;

    // ─────────────────────────────────────────────────────────────────────
    //  NEW: Configurable plan tiering / privileges (Problem #6, #7)
    //  Kept data-driven (admin-configurable via create/update Plan) instead
    //  of hardcoding plan-name checks anywhere in business logic.
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Relative rank of this plan used to decide whether a plan change is an
     * UPGRADE (newPlan.priorityLevel > currentPlan.priorityLevel) or a
     * downgrade/lateral move (not allowed).
     * Higher = better tier. e.g. Basic=1, Standard=2, Premium=3, Elite=4.
     * Default 0 for legacy rows — admin must set this for upgrade logic to
     * treat the plan as a valid tier (fail-closed default).
     */
    @Column(name = "priority_level", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int priorityLevel;

    /**
     * Trainer session discount applied automatically at payment time
     * (Problem #7), expressed as a whole percentage (0-100).
     * e.g. Basic=0, Standard=10, Premium=25, Elite=40.
     */
    @Column(name = "trainer_discount_percent", nullable = false, columnDefinition = "DOUBLE DEFAULT 0")
    private double trainerDiscountPercent;

    /** Whether members on this plan get a dedicated (not shared) trainer. */
    @Column(name = "dedicated_trainer", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean dedicatedTrainer;

    /** Whether members on this plan get priority booking slots. */
    @Column(name = "priority_booking", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean priorityBooking;
}
