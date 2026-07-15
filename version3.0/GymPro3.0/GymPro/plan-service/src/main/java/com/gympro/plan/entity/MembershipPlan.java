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
}
