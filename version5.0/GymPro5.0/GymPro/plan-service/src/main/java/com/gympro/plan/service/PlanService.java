package com.gympro.plan.service;

import com.gympro.plan.dto.FreeSessionCheckResult;
import com.gympro.plan.dto.PlanPrivileges;
import com.gympro.plan.dto.SubscriptionRefundRequest;
import com.gympro.plan.dto.UpgradeQuote;
import com.gympro.plan.entity.MemberFreeSessionUsage;
import com.gympro.plan.entity.MembershipPlan;
import com.gympro.plan.entity.MemberSubscription;
import com.gympro.plan.exception.DuplicateActiveSubscriptionException;
import com.gympro.plan.exception.InvalidUpgradeException;
import com.gympro.plan.exception.PlanDowngradeNotAllowedException;
import com.gympro.plan.exception.PlanNotFoundException;
import com.gympro.plan.exception.SubscriptionNotFoundException;
import com.gympro.plan.feign.NotificationClient;
import com.gympro.plan.feign.PaymentServiceClient;
import com.gympro.plan.repository.MemberFreeSessionUsageRepository;
import com.gympro.plan.repository.MemberSubscriptionRepository;
import com.gympro.plan.repository.MembershipPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);

    @Autowired private MembershipPlanRepository         planRepo;
    @Autowired private MemberSubscriptionRepository     subRepo;
    @Autowired private MemberFreeSessionUsageRepository freeUsageRepo;
    @Autowired private NotificationClient               notificationClient;
    @Autowired private PaymentServiceClient              paymentServiceClient;

    /** First-N-days window during which cancellation is always a 100% refund. */
    private static final int    FULL_REFUND_WINDOW_DAYS = 3;
    /** Beyond this fraction of the plan used, cancellation refund is 0. */
    private static final double NO_REFUND_USAGE_THRESHOLD = 0.80;

    // ─────────────────────────────────────────────────────────────────────────
    //  PLAN CRUD (unchanged)
    // ─────────────────────────────────────────────────────────────────────────

    public MembershipPlan createPlan(MembershipPlan plan) {
        plan.setActive(true);
        if ("MONTHLY".equals(plan.getDurationType()))    plan.setDurationDays(30);
        if ("QUARTERLY".equals(plan.getDurationType()))  plan.setDurationDays(90);
        if ("YEARLY".equals(plan.getDurationType()))     plan.setDurationDays(365);
        return planRepo.save(plan);
    }

    public List<MembershipPlan> getAllPlans() {
        return planRepo.findAll();
    }

    public List<MembershipPlan> getActivePlans() {
        return planRepo.findByActive(true);
    }

    public MembershipPlan getPlanById(Long id) {
        return planRepo.findById(id)
            .orElseThrow(() -> new PlanNotFoundException(id));
    }

    public MembershipPlan updatePlan(Long id, MembershipPlan updated) {
        MembershipPlan plan = getPlanById(id);
        plan.setPlanName(updated.getPlanName());
        plan.setDescription(updated.getDescription());
        plan.setPrice(updated.getPrice());
        plan.setDurationType(updated.getDurationType());
        plan.setDurationDays(updated.getDurationDays());
        plan.setActive(updated.isActive());
        plan.setSessionsIncluded(updated.getSessionsIncluded());
        return planRepo.save(plan);
    }

    public String deactivatePlan(Long id) {
        MembershipPlan plan = getPlanById(id);
        plan.setActive(false);
        planRepo.save(plan);
        return "Plan deactivated ✅";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUBSCRIPTION (unchanged)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Subscribes a member to a plan.
     *
     * Problem #1 / #8 fix: a member can have only ONE active plan at a time.
     * If the member already has an active (non-expired) subscription, this
     * throws DuplicateActiveSubscriptionException — they must go through
     * the upgrade flow (upgradeSubscription) instead of subscribing again.
     * This also inherently prevents overlapping active plans and
     * re-subscribing to the same active plan.
     */
    @Transactional
    public MemberSubscription subscribe(Long memberId, String memberEmail, Long planId) {
        MembershipPlan plan = getPlanById(planId);
        if (!plan.isActive()) {
            throw new InvalidUpgradeException("Plan \"" + plan.getPlanName() + "\" is not currently available");
        }

        MemberSubscription existingActive = resolveActiveSubscription(memberId);
        if (existingActive != null) {
            throw new DuplicateActiveSubscriptionException(
                "Member already has an active plan (\"" + existingActive.getPlanName() + "\"). " +
                "A member can only have ONE active plan at a time — cancel it or use the upgrade flow instead."
            );
        }

        LocalDate start = LocalDate.now();
        LocalDate end   = start.plusDays(plan.getDurationDays());

        MemberSubscription sub = new MemberSubscription();
        sub.setMemberId(memberId);
        sub.setMemberEmail(memberEmail);
        sub.setPlanId(planId);
        sub.setPlanName(plan.getPlanName());
        sub.setStartDate(start);
        sub.setEndDate(end);
        sub.setStatus("ACTIVE");
        sub.setSessionsUsed(0);
        sub.setPlanPriceAtPurchase(plan.getPrice());
        sub.setAmountPaid(plan.getPrice());

        MemberSubscription saved = subRepo.save(sub);

        try {
            notificationClient.sendPlanSubscriptionEmail(
                memberEmail, plan.getPlanName(), start.toString(), end.toString()
            );
        } catch (Exception e) {
            log.warn("⚠️ Plan subscription email failed: {}", e.getMessage());
        }

        return saved;
    }

    public List<MemberSubscription> getMySubscriptions(Long memberId) {
        return subRepo.findByMemberId(memberId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPGRADE  (Problems #2, #3, #6)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Read-only preview of what upgrading to newPlanId would cost, computed
     * from the member's active subscription. Does not mutate anything.
     */
    public UpgradeQuote getUpgradeQuote(Long memberId, Long newPlanId) {
        MemberSubscription activeSub = resolveActiveSubscription(memberId);
        if (activeSub == null) {
            throw new InvalidUpgradeException("Member has no active subscription to upgrade");
        }
        MembershipPlan currentPlan = getPlanById(activeSub.getPlanId());
        MembershipPlan newPlan     = getPlanById(newPlanId);

        validateUpgradeTarget(currentPlan, newPlan);

        double remainingValue = calculateUpgradeCredit(activeSub, currentPlan);
        double amountToPay    = round2(Math.max(0.0, newPlan.getPrice() - remainingValue));

        return new UpgradeQuote(
            activeSub.getId(), currentPlan.getId(), currentPlan.getPlanName(),
            newPlan.getId(), newPlan.getPlanName(), newPlan.getPrice(),
            remainingValue, amountToPay
        );
    }

    /**
     * Performs the actual upgrade:
     *   1. Validates the member has an active subscription and the target
     *      plan is a genuine upgrade (higher priorityLevel — never a
     *      downgrade or lateral move).
     *   2. Computes the prorated amount to pay (new plan price minus the
     *      remaining value of the current plan).
     *   3. Closes out the old subscription (status=UPGRADED — it is no
     *      longer ACTIVE, so it can never be double-counted).
     *   4. Creates the new ACTIVE subscription starting today.
     *
     * The returned MemberSubscription's amountPaid field tells the caller
     * exactly how much to charge via payment-service's existing /payments/pay
     * endpoint (mirrors the existing subscribe-then-pay pattern — no
     * payment-service API changes required).
     */
    @Transactional
    public MemberSubscription upgradeSubscription(Long memberId, String memberEmail, Long newPlanId) {
        MemberSubscription activeSub = resolveActiveSubscription(memberId);
        if (activeSub == null) {
            throw new InvalidUpgradeException("Member has no active subscription to upgrade");
        }
        MembershipPlan currentPlan = getPlanById(activeSub.getPlanId());
        MembershipPlan newPlan     = getPlanById(newPlanId);

        validateUpgradeTarget(currentPlan, newPlan);

        double remainingValue = calculateUpgradeCredit(activeSub, currentPlan);
        double amountToPay    = round2(Math.max(0.0, newPlan.getPrice() - remainingValue));

        // Close out the old subscription — never leave two ACTIVE rows.
        activeSub.setStatus("UPGRADED");
        subRepo.save(activeSub);

        LocalDate start = LocalDate.now();
        LocalDate end   = start.plusDays(newPlan.getDurationDays());

        MemberSubscription newSub = new MemberSubscription();
        newSub.setMemberId(memberId);
        newSub.setMemberEmail(memberEmail);
        newSub.setPlanId(newPlan.getId());
        newSub.setPlanName(newPlan.getPlanName());
        newSub.setStartDate(start);
        newSub.setEndDate(end);
        newSub.setStatus("ACTIVE");
        newSub.setSessionsUsed(0);
        newSub.setPlanPriceAtPurchase(newPlan.getPrice());
        newSub.setAmountPaid(amountToPay);
        newSub.setUpgradedFromSubscriptionId(activeSub.getId());

        MemberSubscription saved = subRepo.save(newSub);

        try {
            notificationClient.sendNotification(
                memberEmail,
                "GymPro – Plan Upgraded",
                "Your plan has been upgraded from \"" + currentPlan.getPlanName() + "\" to \"" +
                newPlan.getPlanName() + "\".\n\n" +
                "Credited from your previous plan: ₹" + remainingValue + "\n" +
                "Amount charged for the upgrade: ₹" + amountToPay + "\n\n" +
                "GymPro Team 💪"
            );
        } catch (Exception e) {
            log.warn("⚠️ Plan upgrade email failed: {}", e.getMessage());
        }

        return saved;
    }

    private void validateUpgradeTarget(MembershipPlan currentPlan, MembershipPlan newPlan) {
        if (!newPlan.isActive()) {
            throw new InvalidUpgradeException("Plan \"" + newPlan.getPlanName() + "\" is not currently available");
        }
        if (newPlan.getId().equals(currentPlan.getId())) {
            throw new InvalidUpgradeException("Member is already subscribed to \"" + newPlan.getPlanName() + "\"");
        }
        if (newPlan.getPriorityLevel() <= currentPlan.getPriorityLevel()) {
            throw new PlanDowngradeNotAllowedException(
                "Downgrade or lateral plan change is not allowed: \"" + currentPlan.getPlanName() +
                "\" (priority " + currentPlan.getPriorityLevel() + ") -> \"" + newPlan.getPlanName() +
                "\" (priority " + newPlan.getPriorityLevel() + "). Only upgrades to a higher-tier plan are permitted."
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CANCELLATION  (Problems #4, #5)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Enterprise cancellation flow:
     *   Cancel Request -> Validate -> Calculate refund -> Deactivate plan
     *   -> Notify payment-service -> Notify member.
     *
     * Refund policy:
     *   - Within the first 3 days of the plan:      100% refund
     *   - More than 80% of the plan duration used:   0% refund
     *   - Otherwise:                                 refund = price * (remainingDays / durationDays)
     */
    @Transactional
    public String cancelSubscription(Long subId) {
        // ── Validate ──────────────────────────────────────────────────────────
        MemberSubscription sub = subRepo.findById(subId)
            .orElseThrow(() -> new SubscriptionNotFoundException(subId));

        if ("CANCELLED".equals(sub.getStatus())) {
            throw new InvalidUpgradeException("Subscription #" + subId + " is already cancelled");
        }
        if ("UPGRADED".equals(sub.getStatus())) {
            throw new InvalidUpgradeException(
                "Subscription #" + subId + " was already upgraded and is no longer active; " +
                "cancel the new (upgraded) subscription instead"
            );
        }

        MembershipPlan plan = planRepo.findById(sub.getPlanId()).orElse(null);

        // ── Calculate refund ─────────────────────────────────────────────────
        double refund = calculateCancellationRefund(sub, plan);

        // ── Deactivate plan ──────────────────────────────────────────────────
        sub.setStatus("CANCELLED");
        sub.setRefundAmount(refund);
        sub.setCancelledAt(LocalDateTime.now());
        subRepo.save(sub);

        // ── Notify payment-service (records the refund against the original payment) ──
        try {
            paymentServiceClient.refundSubscription(
                new SubscriptionRefundRequest(subId, refund, "Plan cancellation — prorated refund")
            );
        } catch (Exception e) {
            log.warn("⚠️ Could not notify payment-service of cancellation refund for subscription {}: {}",
                subId, e.getMessage());
        }

        // ── Notify member ────────────────────────────────────────────────────
        try {
            notificationClient.sendNotification(
                sub.getMemberEmail(),
                "GymPro – Subscription Cancelled",
                "Your subscription to plan \"" + sub.getPlanName() + "\" has been cancelled.\n\n" +
                "Refund amount: ₹" + refund + "\n\n" +
                "If this was a mistake, you can re-subscribe anytime through the app.\n" +
                "GymPro Team 💪"
            );
        } catch (Exception e) {
            log.warn("⚠️ Cancellation email failed: {}", e.getMessage());
        }

        return "Subscription cancelled ✅ — refund: ₹" + refund;
    }

    private double calculateCancellationRefund(MemberSubscription sub, MembershipPlan plan) {
        double price = sub.getPlanPriceAtPurchase() > 0
            ? sub.getPlanPriceAtPurchase()
            : (plan != null ? plan.getPrice() : 0.0);
        int durationDays = plan != null ? plan.getDurationDays() : (int) daysBetween(sub.getStartDate(), sub.getEndDate());
        if (durationDays <= 0 || price <= 0) return 0.0;

        long daysUsed = clamp(daysBetween(sub.getStartDate(), LocalDate.now()), 0, durationDays);

        if (daysUsed <= FULL_REFUND_WINDOW_DAYS) {
            return round2(price);
        }

        double usageFraction = (double) daysUsed / durationDays;
        if (usageFraction > NO_REFUND_USAGE_THRESHOLD) {
            return 0.0;
        }

        long remainingDays = durationDays - daysUsed;
        return round2(price * remainingDays / durationDays);
    }

    /**
     * Upgrade credit = prorated remaining value of the CURRENT plan, with no
     * "first 3 days" or ">80% used" caps (those are cancellation-specific
     * business rules — see Problem #3 vs #4 in the spec).
     */
    private double calculateUpgradeCredit(MemberSubscription sub, MembershipPlan currentPlan) {
        double price = sub.getPlanPriceAtPurchase() > 0 ? sub.getPlanPriceAtPurchase() : currentPlan.getPrice();
        int durationDays = currentPlan.getDurationDays();
        if (durationDays <= 0 || price <= 0) return 0.0;

        long daysUsed = clamp(daysBetween(sub.getStartDate(), LocalDate.now()), 0, durationDays);
        long remainingDays = durationDays - daysUsed;
        return round2(price * remainingDays / durationDays);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PLAN PRIVILEGES  (Problems #6, #7) — data-driven, no hardcoding
    // ─────────────────────────────────────────────────────────────────────────

    public PlanPrivileges getPrivileges(Long memberId) {
        MemberSubscription activeSub = resolveActiveSubscription(memberId);
        if (activeSub == null) return PlanPrivileges.none();

        MembershipPlan plan = planRepo.findById(activeSub.getPlanId()).orElse(null);
        if (plan == null) return PlanPrivileges.none();

        return new PlanPrivileges(
            true, plan.getId(), plan.getPlanName(),
            plan.getPriorityLevel(), plan.getTrainerDiscountPercent(),
            plan.isDedicatedTrainer(), plan.isPriorityBooking()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FREE SESSION CHECK  (UPDATED — now handles non-subscribed case)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a detailed result about free-session eligibility.
     * Called by booking-service via Feign (read-only; does NOT consume a session).
     *
     * Priority order:
     *   1. Active subscription → use sessionsIncluded from plan
     *   2. No subscription     → check MemberFreeSessionUsage (1 lifetime free session)
     *
     * Non-subscribed logic:
     *   - No usage row, or hasUsedFreeSession=false → isFree=true, remaining=1
     *   - hasUsedFreeSession=true                  → isFree=false, remaining=0
     */
    public FreeSessionCheckResult checkFreeSession(Long memberId) {

        // ── Path 1: Active subscription ──────────────────────────────────────
        MemberSubscription activeSub = getActiveSub(memberId);

        if (activeSub != null) {
            MembershipPlan plan = planRepo.findById(activeSub.getPlanId()).orElse(null);
            if (plan == null) {
                return new FreeSessionCheckResult(false, 0, 0, activeSub.getId());
            }

            int included = plan.getSessionsIncluded();
            int used     = activeSub.getSessionsUsed();

            if (included == -1) {
                return new FreeSessionCheckResult(true, -1, -1, activeSub.getId());
            }
            if (included == 0 || used >= included) {
                return new FreeSessionCheckResult(false, 0, included, activeSub.getId());
            }
            int remaining = included - used;
            return new FreeSessionCheckResult(true, remaining, included, activeSub.getId());
        }

        // ── Path 2: No active subscription → 1 lifetime free session ─────────
        MemberFreeSessionUsage usage = freeUsageRepo.findByMemberId(memberId).orElse(null);

        if (usage == null || !usage.isHasUsedFreeSession()) {
            // Member has NOT yet used their 1 free session
            return new FreeSessionCheckResult(true, 1, 1, null);
        }

        // Member already used their 1 free session
        return new FreeSessionCheckResult(false, 0, 1, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  USE SESSION  (UPDATED — handles non-subscribed path)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Atomically consumes one free session.
     * Returns true if a session was consumed, false if none were available.
     *
     * Non-subscribed path:
     *   - Creates or updates MemberFreeSessionUsage row.
     *   - Sets hasUsedFreeSession=true so the next booking requires payment.
     */
    @Transactional
    public boolean useIncludedSession(Long memberId) {

        // ── Path 1: Active subscription ──────────────────────────────────────
        // Locked lookup: without this, two concurrent booking requests for the
        // same member can both read sessionsUsed=N and both save N+1, letting
        // the member consume more free sessions than their plan allows.
        MemberSubscription activeSub = getActiveSubForUpdate(memberId);

        if (activeSub != null) {
            MembershipPlan plan = planRepo.findById(activeSub.getPlanId()).orElse(null);
            if (plan == null) return false;

            int included = plan.getSessionsIncluded();
            if (included == -1) return true; // unlimited — no counter update

            int used = activeSub.getSessionsUsed();
            if (used < included) {
                activeSub.setSessionsUsed(used + 1);
                subRepo.save(activeSub);
                log.info("✅ Subscription free session consumed: memberId={}, used={}/{}", memberId, used + 1, included);
                return true;
            }
            return false; // quota exhausted
        }

        // ── Path 2: No subscription → consume the 1 lifetime free session ────
        // Locked lookup: same race, but for the non-subscribed hasUsedFreeSession flag.
        MemberFreeSessionUsage usage = freeUsageRepo.findByMemberIdForUpdate(memberId)
            .orElseGet(() -> {
                MemberFreeSessionUsage newUsage = new MemberFreeSessionUsage();
                newUsage.setMemberId(memberId);
                newUsage.setHasUsedFreeSession(false);
                return newUsage;
            });

        if (usage.isHasUsedFreeSession()) {
            // Already used the 1 free session — cannot consume again
            return false;
        }

        // Mark as consumed
        usage.setHasUsedFreeSession(true);
        usage.setUsedAt(LocalDateTime.now());
        freeUsageRepo.save(usage);
        log.info("✅ Non-subscribed free session consumed: memberId={}", memberId);
        return true;
    }

    /**
     * Updates the bookingId audit field on the free-session usage record.
     * Called after the booking is saved to link the free session to the booking.
     * Safe to call regardless of subscription status (no-op if subscribed).
     */
    @Transactional
    public void linkFreeSessionToBooking(Long memberId, Long bookingId) {
        freeUsageRepo.findByMemberId(memberId).ifPresent(usage -> {
            usage.setBookingId(bookingId);
            freeUsageRepo.save(usage);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RESTORE SESSION  (UPDATED — handles non-subscribed path)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when a FREE_SESSION booking is cancelled.
     * Restores the consumed free session so the member can book again for free.
     *
     * Non-subscribed path: resets hasUsedFreeSession=false.
     * Subscribed path:     decrements sessionsUsed (existing logic).
     */
    @Transactional
    public void restoreFreeSession(Long memberId) {

        // ── Path 1: Active subscription ──────────────────────────────────────
        MemberSubscription activeSub = getActiveSub(memberId);

        if (activeSub != null) {
            MembershipPlan plan = planRepo.findById(activeSub.getPlanId()).orElse(null);
            if (plan == null) return;

            int included = plan.getSessionsIncluded();
            if (included == -1) return; // unlimited — nothing to restore

            int used = activeSub.getSessionsUsed();
            if (used > 0) {
                activeSub.setSessionsUsed(used - 1);
                subRepo.save(activeSub);
                log.info("✅ Subscription free session restored for memberId={}", memberId);
            }
            return;
        }

        // ── Path 2: No subscription → restore the lifetime free session ───────
        MemberFreeSessionUsage usage = freeUsageRepo.findByMemberId(memberId).orElse(null);
        if (usage != null && usage.isHasUsedFreeSession()) {
            usage.setHasUsedFreeSession(false);
            usage.setUsedAt(null);
            usage.setBookingId(null);
            freeUsageRepo.save(usage);
            log.info("✅ Non-subscribed free session restored for memberId={}", memberId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADMIN: get free session usage status for a member
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the MemberFreeSessionUsage record for a non-subscribed member.
     * Returns null if no record exists (member has never attempted a booking).
     */
    public MemberFreeSessionUsage getFreeSessionUsage(Long memberId) {
        return freeUsageRepo.findByMemberId(memberId).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private MemberSubscription getActiveSub(Long memberId) {
        return resolveActiveSubscription(memberId);
    }

    /**
     * Shared active-subscription lookup used by subscribe / upgrade / cancel /
     * free-session logic. A row with status=ACTIVE but an endDate in the past
     * is treated as not active (auto-expiry), which also means a member whose
     * plan has lapsed is free to subscribe to a new plan.
     */
    private MemberSubscription resolveActiveSubscription(Long memberId) {
        List<MemberSubscription> subs = subRepo.findByMemberId(memberId);
        return subs.stream()
            .filter(s -> "ACTIVE".equals(s.getStatus()) && !s.getEndDate().isBefore(LocalDate.now()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Same lookup as getActiveSub(), but takes a PESSIMISTIC_WRITE row lock.
     * Only used by useIncludedSession(), which reads sessionsUsed and then
     * writes it back — the lock is what makes that read-then-write safe
     * under concurrent requests for the same member.
     */
    private MemberSubscription getActiveSubForUpdate(Long memberId) {
        List<MemberSubscription> subs = subRepo.findByMemberIdForUpdate(memberId);
        return subs.stream()
            .filter(s -> "ACTIVE".equals(s.getStatus()) && !s.getEndDate().isBefore(LocalDate.now()))
            .findFirst()
            .orElse(null);
    }

    private long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        return ChronoUnit.DAYS.between(start, end);
    }

    private long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Rounds DOWN to 2 decimal places — refund/credit math favors precision without over-crediting. */
    private double round2(double value) {
        return Math.floor(value * 100.0) / 100.0;
    }
}
