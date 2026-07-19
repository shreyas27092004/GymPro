package com.gympro.plan.controller;

import com.gympro.plan.dto.FreeSessionCheckResult;
import com.gympro.plan.dto.PlanPrivileges;
import com.gympro.plan.dto.UpgradeQuote;
import com.gympro.plan.entity.MemberFreeSessionUsage;
import com.gympro.plan.entity.MembershipPlan;
import com.gympro.plan.entity.MemberSubscription;
import com.gympro.plan.exception.AccessDeniedException;
import com.gympro.plan.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plans")
@Tag(name = "Plans", description = "Membership plan catalogue and member subscriptions")
public class PlanController {

    @Autowired
    private PlanService service;

    // ── Create Plan ───────────────────────────────────────────────────────────

    @Operation(
        summary     = "Create a membership plan (Admin only)",
        description = "Adds a new plan. sessionsIncluded: 0=none, N=N free sessions, -1=unlimited. Requires X-User-Role: ADMIN."
    )
    @PostMapping
    public MembershipPlan createPlan(
            @RequestBody MembershipPlan plan,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException("Access denied ❌ ADMIN only");
        return service.createPlan(plan);
    }

    // ── Get All Plans ─────────────────────────────────────────────────────────

    @GetMapping
    public List<MembershipPlan> getAll() {
        return service.getActivePlans();
    }

    // ── Get Plan by ID ────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public MembershipPlan getById(@PathVariable Long id) {
        return service.getPlanById(id);
    }

    // ── Update Plan ───────────────────────────────────────────────────────────

    @Operation(summary = "Update a plan (Admin only)")
    @PutMapping("/{id}")
    public MembershipPlan update(
            @PathVariable Long id,
            @RequestBody MembershipPlan plan,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException("Access denied ❌ ADMIN only");
        return service.updatePlan(id, plan);
    }

    // ── Deactivate Plan ───────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public String deactivate(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException("Access denied ❌ ADMIN only");
        return service.deactivatePlan(id);
    }

    // ── Subscribe ─────────────────────────────────────────────────────────────

    @PostMapping("/subscribe")
    public MemberSubscription subscribe(
            @RequestParam Long memberId,
            @RequestParam String memberEmail,
            @RequestParam Long planId) {
        return service.subscribe(memberId, memberEmail, planId);
    }

    // ── Get My Subscriptions ──────────────────────────────────────────────────

    @GetMapping("/my/{memberId}")
    public List<MemberSubscription> myPlans(@PathVariable Long memberId) {
        return service.getMySubscriptions(memberId);
    }

    // ── Upgrade Quote (preview — no side effects) ────────────────────────────

    @Operation(
        summary     = "Preview the prorated cost of upgrading to a new plan",
        description = """
                      Read-only. Computes remainingValue = currentPlanPrice * (remainingDays / durationDays)
                      and amountToPay = newPlanPrice - remainingValue (floored at 0).
                      Throws 400 if the target plan is not a strict upgrade
                      (newPlan.priorityLevel must be > currentPlan.priorityLevel).
                      """
    )
    @GetMapping("/upgrade-quote")
    public UpgradeQuote upgradeQuote(
            @RequestParam Long memberId,
            @RequestParam Long newPlanId) {
        return service.getUpgradeQuote(memberId, newPlanId);
    }

    // ── Upgrade ────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Upgrade a member's active plan",
        description = """
                      Closes the member's current ACTIVE subscription (status=UPGRADED)
                      and creates a new ACTIVE subscription for the higher-tier plan.
                      Downgrades and lateral moves are rejected with 400.
                      The response's amountPaid field is what should be charged via
                      POST /payments/pay (mirrors the existing subscribe-then-pay flow).
                      """
    )
    @PostMapping("/upgrade")
    public MemberSubscription upgrade(
            @RequestParam Long memberId,
            @RequestParam String memberEmail,
            @RequestParam Long newPlanId) {
        return service.upgradeSubscription(memberId, memberEmail, newPlanId);
    }

    // ── Plan Privileges ────────────────────────────────────────────────────────

    @Operation(
        summary     = "Get the configured privileges of a member's active plan",
        description = "Data-driven — priorityLevel, trainerDiscountPercent, dedicatedTrainer, " +
                      "priorityBooking all come from the MembershipPlan an admin configured. " +
                      "Returns hasActivePlan=false with no-privilege defaults if the member has no active plan."
    )
    @GetMapping("/privileges/{memberId}")
    public PlanPrivileges privileges(@PathVariable Long memberId) {
        return service.getPrivileges(memberId);
    }

    // ── Check Free Session ────────────────────────────────────────────────────

    @Operation(
        summary     = "Check if a member has free sessions remaining",
        description = """
                      Returns eligibility WITHOUT consuming a session.
                      
                      Two cases:
                        1. SUBSCRIBED: uses sessionsIncluded from the active plan.
                           sessionsIncluded=-1 → unlimited (isFree=true, remaining=-1)
                           sessionsIncluded=0  → no free sessions (isFree=false)
                           sessionsIncluded=N  → N minus sessionsUsed free sessions remaining
                        2. NOT SUBSCRIBED: member gets exactly 1 lifetime free session.
                           If not yet used: isFree=true, remaining=1
                           If already used: isFree=false, remaining=0
                      """
    )
    @ApiResponse(responseCode = "200", description = "Free session check result")
    @GetMapping("/free-session-check/{memberId}")
    public FreeSessionCheckResult checkFreeSession(@PathVariable Long memberId) {
        return service.checkFreeSession(memberId);
    }

    // ── Use Session (consume one free session) ────────────────────────────────

    @Operation(
        summary     = "Consume one included free session",
        description = """
                      Atomically marks one free session as used.
                      - Subscribed: increments sessionsUsed.
                      - Non-subscribed: sets hasUsedFreeSession=true.
                      Returns true if consumed, false if none available.
                      """
    )
    @PostMapping("/use-session/{memberId}")
    public boolean useIncludedSession(@PathVariable Long memberId) {
        return service.useIncludedSession(memberId);
    }

    // ── Restore Free Session ──────────────────────────────────────────────────

    @Operation(
        summary     = "Restore one free session (called on FREE booking cancellation)",
        description = """
                      Reverses a previously consumed free session.
                      - Subscribed: decrements sessionsUsed.
                      - Non-subscribed: resets hasUsedFreeSession=false.
                      """
    )
    @PostMapping("/restore-session/{memberId}")
    public String restoreFreeSession(@PathVariable Long memberId) {
        service.restoreFreeSession(memberId);
        return "Free session restored ✅";
    }

    // ── Get Free Session Usage (non-subscribed member audit) ──────────────────

    @Operation(
        summary     = "Get free session usage for a non-subscribed member (Admin only)",
        description = "Returns the MemberFreeSessionUsage record. Null if member never attempted a booking."
    )
    @GetMapping("/free-session-usage/{memberId}")
    public MemberFreeSessionUsage getFreeSessionUsage(
            @PathVariable Long memberId,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException("Access denied ❌ ADMIN only");
        return service.getFreeSessionUsage(memberId);
    }

    // ── Cancel Subscription ───────────────────────────────────────────────────

    @DeleteMapping("/subscription/{subId}")
    public String cancel(@PathVariable Long subId) {
        return service.cancelSubscription(subId);
    }

    // ── Health check ──────────────────────────────────────────────────────────

    @GetMapping("/test")
    public String test() { return "Plan Service Working ✅"; }
}
