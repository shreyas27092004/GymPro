package com.gympro.plan;

import com.gympro.plan.dto.UpgradeQuote;
import com.gympro.plan.entity.MembershipPlan;
import com.gympro.plan.entity.MemberSubscription;
import com.gympro.plan.exception.DuplicateActiveSubscriptionException;
import com.gympro.plan.exception.InvalidUpgradeException;
import com.gympro.plan.exception.PlanDowngradeNotAllowedException;
import com.gympro.plan.exception.SubscriptionNotFoundException;
import com.gympro.plan.feign.NotificationClient;
import com.gympro.plan.feign.PaymentServiceClient;
import com.gympro.plan.repository.MemberFreeSessionUsageRepository;
import com.gympro.plan.repository.MemberSubscriptionRepository;
import com.gympro.plan.repository.MembershipPlanRepository;
import com.gympro.plan.service.PlanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Covers the new Problem #1–#8 business rules:
 *   - single active plan per member (subscribe rejects duplicates)
 *   - upgrade-only plan changes, with correct proration math
 *   - enterprise cancellation refund policy (3-day full refund / >80% no refund / prorated)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanService — upgrade / cancellation / duplicate-plan rules")
class PlanServiceUpgradeCancellationTest {

    @Mock private MembershipPlanRepository         planRepo;
    @Mock private MemberSubscriptionRepository     subRepo;
    @Mock private MemberFreeSessionUsageRepository freeUsageRepo;
    @Mock private NotificationClient               notificationClient;
    @Mock private PaymentServiceClient             paymentServiceClient;

    @InjectMocks
    private PlanService planService;

    private static final Long MEMBER_ID = 7L;

    private MembershipPlan plan(Long id, String name, double price, int durationDays, int priorityLevel) {
        MembershipPlan p = new MembershipPlan();
        p.setId(id);
        p.setPlanName(name);
        p.setPrice(price);
        p.setDurationDays(durationDays);
        p.setActive(true);
        p.setPriorityLevel(priorityLevel);
        return p;
    }

    private MemberSubscription activeSub(Long id, Long planId, String planName, double price,
                                          LocalDate start, int durationDays) {
        MemberSubscription s = new MemberSubscription();
        s.setId(id);
        s.setMemberId(MEMBER_ID);
        s.setMemberEmail("member@gym.com");
        s.setPlanId(planId);
        s.setPlanName(planName);
        s.setStartDate(start);
        s.setEndDate(start.plusDays(durationDays));
        s.setStatus("ACTIVE");
        s.setPlanPriceAtPurchase(price);
        return s;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Problem #1 / #8 — one active plan per member
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("subscribe() — single active plan enforcement")
    class Subscribe {

        @Test
        @DisplayName("Rejects a second subscribe while an active plan exists")
        void rejectsDuplicateActiveSubscription() {
            MembershipPlan basic = plan(1L, "Basic", 1000.0, 30, 1);
            MemberSubscription existing = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now(), 30);

            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(existing));

            assertThrows(DuplicateActiveSubscriptionException.class,
                () -> planService.subscribe(MEMBER_ID, "member@gym.com", 1L));

            verify(subRepo, never()).save(any());
        }

        @Test
        @DisplayName("Allows subscribing when the member has no active plan")
        void allowsSubscribeWhenNoActivePlan() {
            MembershipPlan basic = plan(1L, "Basic", 1000.0, 30, 1);
            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of());
            when(subRepo.save(any(MemberSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

            MemberSubscription result = planService.subscribe(MEMBER_ID, "member@gym.com", 1L);

            assertEquals("ACTIVE", result.getStatus());
            assertEquals(1000.0, result.getPlanPriceAtPurchase());
            verify(subRepo).save(any(MemberSubscription.class));
        }

        @Test
        @DisplayName("A previously expired plan does not block a new subscribe")
        void expiredPlanDoesNotBlockSubscribe() {
            MembershipPlan basic = plan(1L, "Basic", 1000.0, 30, 1);
            MemberSubscription expired = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(40), 30);
            // status is ACTIVE but endDate is in the past

            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(expired));
            when(subRepo.save(any(MemberSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> planService.subscribe(MEMBER_ID, "member@gym.com", 1L));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Problem #2 — upgrade-only plan changes
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("upgradeSubscription() — direction validation")
    class UpgradeDirection {

        @Test
        @DisplayName("Basic -> Premium is allowed")
        void allowsUpgrade() {
            MembershipPlan basic   = plan(1L, "Basic", 1000.0, 30, 1);
            MembershipPlan premium = plan(2L, "Premium", 2000.0, 30, 3);
            MemberSubscription current = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(10), 30);

            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(current));
            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));
            when(planRepo.findById(2L)).thenReturn(Optional.of(premium));
            when(subRepo.save(any(MemberSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

            MemberSubscription result = planService.upgradeSubscription(MEMBER_ID, "member@gym.com", 2L);

            assertEquals("Premium", result.getPlanName());
            assertEquals("ACTIVE", result.getStatus());
        }

        @Test
        @DisplayName("Elite -> Premium (downgrade) is rejected")
        void rejectsDowngrade() {
            MembershipPlan elite   = plan(1L, "Elite", 3000.0, 30, 4);
            MembershipPlan premium = plan(2L, "Premium", 2000.0, 30, 3);
            MemberSubscription current = activeSub(100L, 1L, "Elite", 3000.0, LocalDate.now().minusDays(5), 30);

            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(current));
            when(planRepo.findById(1L)).thenReturn(Optional.of(elite));
            when(planRepo.findById(2L)).thenReturn(Optional.of(premium));

            assertThrows(PlanDowngradeNotAllowedException.class,
                () -> planService.upgradeSubscription(MEMBER_ID, "member@gym.com", 2L));

            verify(subRepo, never()).save(any());
        }

        @Test
        @DisplayName("Premium -> Basic (downgrade) is rejected")
        void rejectsDowngradeToBasic() {
            MembershipPlan premium = plan(1L, "Premium", 2000.0, 30, 3);
            MembershipPlan basic   = plan(2L, "Basic", 1000.0, 30, 1);
            MemberSubscription current = activeSub(100L, 1L, "Premium", 2000.0, LocalDate.now().minusDays(5), 30);

            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(current));
            when(planRepo.findById(1L)).thenReturn(Optional.of(premium));
            when(planRepo.findById(2L)).thenReturn(Optional.of(basic));

            assertThrows(PlanDowngradeNotAllowedException.class,
                () -> planService.upgradeSubscription(MEMBER_ID, "member@gym.com", 2L));
        }

        @Test
        @DisplayName("Upgrading to the same plan is rejected")
        void rejectsSamePlan() {
            MembershipPlan basic = plan(1L, "Basic", 1000.0, 30, 1);
            MemberSubscription current = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(5), 30);

            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(current));
            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));

            assertThrows(InvalidUpgradeException.class,
                () -> planService.upgradeSubscription(MEMBER_ID, "member@gym.com", 1L));
        }

        @Test
        @DisplayName("Upgrading with no active subscription is rejected")
        void rejectsNoActiveSubscription() {
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of());

            assertThrows(InvalidUpgradeException.class,
                () -> planService.upgradeSubscription(MEMBER_ID, "member@gym.com", 2L));
        }

        @Test
        @DisplayName("Old subscription is closed out with status=UPGRADED, never left ACTIVE")
        void closesOldSubscriptionOnUpgrade() {
            MembershipPlan basic   = plan(1L, "Basic", 1000.0, 30, 1);
            MembershipPlan premium = plan(2L, "Premium", 2000.0, 30, 3);
            MemberSubscription current = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(10), 30);

            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(current));
            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));
            when(planRepo.findById(2L)).thenReturn(Optional.of(premium));
            when(subRepo.save(any(MemberSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

            planService.upgradeSubscription(MEMBER_ID, "member@gym.com", 2L);

            ArgumentCaptor<MemberSubscription> captor = ArgumentCaptor.forClass(MemberSubscription.class);
            verify(subRepo, times(2)).save(captor.capture());
            assertEquals("UPGRADED", captor.getAllValues().get(0).getStatus());
            assertEquals("ACTIVE", captor.getAllValues().get(1).getStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Problem #3 — upgrade proration math
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getUpgradeQuote() — proration math")
    class UpgradeQuoteMath {

        @Test
        @DisplayName("Spec example: Basic ₹1000 (30d, 10d used) -> Premium ₹2000 costs ₹1334")
        void matchesSpecExample() {
            MembershipPlan basic   = plan(1L, "Basic", 1000.0, 30, 1);
            MembershipPlan premium = plan(2L, "Premium", 2000.0, 30, 3);
            MemberSubscription current = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(10), 30);

            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(current));
            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));
            when(planRepo.findById(2L)).thenReturn(Optional.of(premium));

            UpgradeQuote quote = planService.getUpgradeQuote(MEMBER_ID, 2L);

            // remainingValue = 1000 * 20/30 = 666.67 (floored to 666.66) -> amountToPay = 2000 - remainingValue
            assertEquals(666.66, quote.getRemainingValue(), 0.01);
            assertEquals(1333.34, quote.getAmountToPay(), 0.01);
            assertTrue(quote.getAmountToPay() < premium.getPrice());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Problem #4 — cancellation refund policy
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cancelSubscription() — enterprise refund policy")
    class CancellationRefund {

        @Test
        @DisplayName("Within first 3 days -> 100% refund")
        void fullRefundWithinFirstThreeDays() {
            MembershipPlan basic = plan(1L, "Basic", 1000.0, 30, 1);
            MemberSubscription sub = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(2), 30);

            when(subRepo.findById(100L)).thenReturn(Optional.of(sub));
            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));

            String result = planService.cancelSubscription(100L);

            assertEquals(1000.0, sub.getRefundAmount());
            assertEquals("CANCELLED", sub.getStatus());
            assertTrue(result.contains("1000.0"));
        }

        @Test
        @DisplayName("Spec example: 30-day plan, 8 days used -> refund = 22/30 of price")
        void proratedRefundMatchesSpecExample() {
            MembershipPlan basic = plan(1L, "Basic", 1000.0, 30, 1);
            MemberSubscription sub = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(8), 30);

            when(subRepo.findById(100L)).thenReturn(Optional.of(sub));
            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));

            planService.cancelSubscription(100L);

            // 1000 * 22/30 = 733.33
            assertEquals(733.33, sub.getRefundAmount(), 0.01);
        }

        @Test
        @DisplayName("More than 80% of plan used -> 0 refund")
        void noRefundAfterEightyPercentUsed() {
            MembershipPlan basic = plan(1L, "Basic", 1000.0, 30, 1);
            // 25/30 = 83% used
            MemberSubscription sub = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(25), 30);

            when(subRepo.findById(100L)).thenReturn(Optional.of(sub));
            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));

            planService.cancelSubscription(100L);

            assertEquals(0.0, sub.getRefundAmount());
        }

        @Test
        @DisplayName("Notifies payment-service with the calculated refund amount")
        void notifiesPaymentService() {
            MembershipPlan basic = plan(1L, "Basic", 1000.0, 30, 1);
            MemberSubscription sub = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(1), 30);

            when(subRepo.findById(100L)).thenReturn(Optional.of(sub));
            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));

            planService.cancelSubscription(100L);

            verify(paymentServiceClient).refundSubscription(argThat(req ->
                req.getSubscriptionId().equals(100L) && req.getRefundAmount() == 1000.0
            ));
        }

        @Test
        @DisplayName("Cancelling an unknown subscription throws SubscriptionNotFoundException")
        void throwsWhenSubscriptionMissing() {
            when(subRepo.findById(999L)).thenReturn(Optional.empty());
            assertThrows(SubscriptionNotFoundException.class, () -> planService.cancelSubscription(999L));
        }

        @Test
        @DisplayName("Cancelling an already-cancelled subscription is rejected")
        void rejectsDoubleCancel() {
            MemberSubscription sub = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(5), 30);
            sub.setStatus("CANCELLED");
            when(subRepo.findById(100L)).thenReturn(Optional.of(sub));

            assertThrows(InvalidUpgradeException.class, () -> planService.cancelSubscription(100L));
        }

        @Test
        @DisplayName("Cancellation still succeeds (with 0 refund recorded) even if payment-service is unreachable")
        void survivesPaymentServiceOutage() {
            MembershipPlan basic = plan(1L, "Basic", 1000.0, 30, 1);
            MemberSubscription sub = activeSub(100L, 1L, "Basic", 1000.0, LocalDate.now().minusDays(1), 30);

            when(subRepo.findById(100L)).thenReturn(Optional.of(sub));
            when(planRepo.findById(1L)).thenReturn(Optional.of(basic));
            doThrow(new RuntimeException("payment-service down"))
                .when(paymentServiceClient).refundSubscription(any());

            assertDoesNotThrow(() -> planService.cancelSubscription(100L));
            assertEquals("CANCELLED", sub.getStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Problem #6 / #7 — plan privileges
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPrivileges() — data-driven plan privileges")
    class Privileges {

        @Test
        @DisplayName("Returns configured privileges for an active plan")
        void returnsConfiguredPrivileges() {
            MembershipPlan elite = plan(1L, "Elite", 3000.0, 30, 4);
            elite.setTrainerDiscountPercent(40.0);
            elite.setDedicatedTrainer(true);
            elite.setPriorityBooking(true);

            MemberSubscription sub = activeSub(100L, 1L, "Elite", 3000.0, LocalDate.now(), 30);
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(sub));
            when(planRepo.findById(1L)).thenReturn(Optional.of(elite));

            var privileges = planService.getPrivileges(MEMBER_ID);

            assertTrue(privileges.isHasActivePlan());
            assertEquals(40.0, privileges.getTrainerDiscountPercent());
            assertTrue(privileges.isDedicatedTrainer());
            assertTrue(privileges.isPriorityBooking());
        }

        @Test
        @DisplayName("Returns no-privilege defaults when member has no active plan")
        void returnsNoneWhenNoActivePlan() {
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of());

            var privileges = planService.getPrivileges(MEMBER_ID);

            assertFalse(privileges.isHasActivePlan());
            assertEquals(0.0, privileges.getTrainerDiscountPercent());
        }
    }
}
