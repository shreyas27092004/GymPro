package com.gympro.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private String memberEmail;

    private Long planId;
    private String planName;

    private LocalDate startDate;
    private LocalDate endDate;
    private String status;          // ACTIVE | EXPIRED | CANCELLED | UPGRADED

    // Tracks how many included sessions have been used
    private int sessionsUsed;

    // ─────────────────────────────────────────────────────────────────────
    //  NEW fields for upgrade proration + cancellation refund tracking
    //  (Problems #3 and #4). All nullable/defaulted for backward
    //  compatibility with existing rows.
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Snapshot of the plan's price at the moment this subscription row was
     * created (subscribe OR upgrade). Refund/upgrade-credit math is always
     * based on this snapshot, not the plan's current (possibly since-changed)
     * price, so historical calculations stay stable.
     */
    @Column(name = "plan_price_at_purchase", columnDefinition = "DOUBLE DEFAULT 0")
    private double planPriceAtPurchase;

    /**
     * The actual amount the member paid to enter this subscription/upgrade
     * (full price for a fresh subscribe, or the prorated "amount to pay" for
     * an upgrade). Used only for display/audit — refund math uses
     * planPriceAtPurchase, per the enterprise cancellation policy.
     */
    @Column(name = "amount_paid")
    private Double amountPaid;

    /** Refund computed when this subscription was cancelled (null until cancelled). */
    @Column(name = "refund_amount")
    private Double refundAmount;

    /** Timestamp this subscription was cancelled (null unless CANCELLED). */
    private LocalDateTime cancelledAt;

    /**
     * If this subscription was created by upgrading from a previous one,
     * the ID of that previous (now UPGRADED) subscription. Null for a
     * fresh subscribe.
     */
    @Column(name = "upgraded_from_subscription_id")
    private Long upgradedFromSubscriptionId;
}