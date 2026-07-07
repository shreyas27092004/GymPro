package com.gympro.plan.service;

import com.gympro.plan.dto.FreeSessionCheckResult;
import com.gympro.plan.entity.MemberFreeSessionUsage;
import com.gympro.plan.entity.MembershipPlan;
import com.gympro.plan.entity.MemberSubscription;
import com.gympro.plan.exception.PlanNotFoundException;
import com.gympro.plan.exception.SubscriptionNotFoundException;
import com.gympro.plan.feign.NotificationClient;
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
import java.util.List;

@Service
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);

    @Autowired private MembershipPlanRepository         planRepo;
    @Autowired private MemberSubscriptionRepository     subRepo;
    @Autowired private MemberFreeSessionUsageRepository freeUsageRepo;
    @Autowired private NotificationClient               notificationClient;

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

    public MemberSubscription subscribe(Long memberId, String memberEmail, Long planId) {
        MembershipPlan plan = getPlanById(planId);

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

    public String cancelSubscription(Long subId) {
        MemberSubscription sub = subRepo.findById(subId)
            .orElseThrow(() -> new SubscriptionNotFoundException(subId));
        sub.setStatus("CANCELLED");
        subRepo.save(sub);

        try {
            notificationClient.sendNotification(
                sub.getMemberEmail(),
                "GymPro – Subscription Cancelled",
                "Your subscription to plan \"" + sub.getPlanName() + "\" has been cancelled.\n\n" +
                "If this was a mistake, you can re-subscribe anytime through the app.\n" +
                "GymPro Team 💪"
            );
        } catch (Exception e) {
            log.warn("⚠️ Cancellation email failed: {}", e.getMessage());
        }

        return "Subscription cancelled ✅";
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
        MemberSubscription activeSub = getActiveSub(memberId);

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
        MemberFreeSessionUsage usage = freeUsageRepo.findByMemberId(memberId)
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
        List<MemberSubscription> subs = subRepo.findByMemberId(memberId);
        return subs.stream()
            .filter(s -> "ACTIVE".equals(s.getStatus()) && !s.getEndDate().isBefore(LocalDate.now()))
            .findFirst()
            .orElse(null);
    }
}
