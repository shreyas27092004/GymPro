package com.gympro.plan;

import com.gympro.plan.dto.FreeSessionCheckResult;
import com.gympro.plan.entity.MemberFreeSessionUsage;
import com.gympro.plan.entity.MembershipPlan;
import com.gympro.plan.entity.MemberSubscription;
import com.gympro.plan.feign.NotificationClient;
import com.gympro.plan.repository.MemberFreeSessionUsageRepository;
import com.gympro.plan.repository.MemberSubscriptionRepository;
import com.gympro.plan.repository.MembershipPlanRepository;
import com.gympro.plan.service.PlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanService — free session rules")
public class PlanServiceTest {

    @Mock private MembershipPlanRepository         planRepo;
    @Mock private MemberSubscriptionRepository     subRepo;
    @Mock private MemberFreeSessionUsageRepository freeUsageRepo;
    @Mock private NotificationClient               notificationClient;

    @InjectMocks
    private PlanService planService;

    private static final Long MEMBER_ID = 42L;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private MemberSubscription activeSub(int sessionsIncluded, int sessionsUsed) {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(1L);
        plan.setSessionsIncluded(sessionsIncluded);

        MemberSubscription sub = new MemberSubscription();
        sub.setId(100L);
        sub.setMemberId(MEMBER_ID);
        sub.setPlanId(1L);
        sub.setPlanName("Test Plan");
        sub.setStartDate(LocalDate.now().minusDays(10));
        sub.setEndDate(LocalDate.now().plusDays(20));
        sub.setStatus("ACTIVE");
        sub.setSessionsUsed(sessionsUsed);

        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        return sub;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  checkFreeSession — NON-SUBSCRIBED
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkFreeSession — non-subscribed member")
    class CheckFreeSession_NonSubscribed {

        @BeforeEach
        void noSubscription() {
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("Returns isFree=true when no usage record exists (never booked before)")
        void noUsageRecord_returnsFree() {
            when(freeUsageRepo.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());

            FreeSessionCheckResult result = planService.checkFreeSession(MEMBER_ID);

            assertTrue(result.isFree());
            assertEquals(1, result.getRemainingFreeSessions());
            assertEquals(1, result.getTotalFreeSessions());
        }

        @Test
        @DisplayName("Returns isFree=true when usage record exists but free session not yet used")
        void usageRecord_notUsed_returnsFree() {
            MemberFreeSessionUsage usage = new MemberFreeSessionUsage();
            usage.setMemberId(MEMBER_ID);
            usage.setHasUsedFreeSession(false);
            when(freeUsageRepo.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(usage));

            FreeSessionCheckResult result = planService.checkFreeSession(MEMBER_ID);

            assertTrue(result.isFree());
            assertEquals(1, result.getRemainingFreeSessions());
        }

        @Test
        @DisplayName("Returns isFree=false when free session already used")
        void usageRecord_alreadyUsed_returnsPaid() {
            MemberFreeSessionUsage usage = new MemberFreeSessionUsage();
            usage.setMemberId(MEMBER_ID);
            usage.setHasUsedFreeSession(true);
            when(freeUsageRepo.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(usage));

            FreeSessionCheckResult result = planService.checkFreeSession(MEMBER_ID);

            assertFalse(result.isFree());
            assertEquals(0, result.getRemainingFreeSessions());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  checkFreeSession — SUBSCRIBED
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("checkFreeSession — subscribed member")
    class CheckFreeSession_Subscribed {

        @Test
        @DisplayName("Unlimited plan (-1) always returns isFree=true with remaining=-1")
        void unlimitedPlan_alwaysFree() {
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(activeSub(-1, 0)));

            FreeSessionCheckResult result = planService.checkFreeSession(MEMBER_ID);

            assertTrue(result.isFree());
            assertEquals(-1, result.getRemainingFreeSessions());
        }

        @Test
        @DisplayName("Zero sessions plan returns isFree=false")
        void zeroSessionsPlan_neverFree() {
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(activeSub(0, 0)));

            FreeSessionCheckResult result = planService.checkFreeSession(MEMBER_ID);

            assertFalse(result.isFree());
            assertEquals(0, result.getRemainingFreeSessions());
        }

        @Test
        @DisplayName("Has 5 included, used 2 → remaining=3, isFree=true")
        void partiallyUsed_hasSessions() {
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(activeSub(5, 2)));

            FreeSessionCheckResult result = planService.checkFreeSession(MEMBER_ID);

            assertTrue(result.isFree());
            assertEquals(3, result.getRemainingFreeSessions());
        }

        @Test
        @DisplayName("Has 5 included, used 5 → isFree=false, remaining=0")
        void fullyUsed_noSessions() {
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(activeSub(5, 5)));

            FreeSessionCheckResult result = planService.checkFreeSession(MEMBER_ID);

            assertFalse(result.isFree());
            assertEquals(0, result.getRemainingFreeSessions());
        }

        @Test
        @DisplayName("Over-used (used > included) still returns isFree=false")
        void overUsed_noSessions() {
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(activeSub(3, 5)));

            FreeSessionCheckResult result = planService.checkFreeSession(MEMBER_ID);

            assertFalse(result.isFree());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  useIncludedSession — NON-SUBSCRIBED
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("useIncludedSession — non-subscribed member")
    class UseSession_NonSubscribed {

        @BeforeEach
        void noSubscription() {
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("Creates new usage record and returns true when member has never booked")
        void noUsageRecord_createsAndReturnsTrue() {
            when(freeUsageRepo.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());
            when(freeUsageRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            boolean result = planService.useIncludedSession(MEMBER_ID);

            assertTrue(result);
            verify(freeUsageRepo).save(argThat(u -> u.isHasUsedFreeSession() && u.getUsedAt() != null));
        }

        @Test
        @DisplayName("Updates existing record and returns true when free session not yet used")
        void existingRecord_notUsed_returnsTrue() {
            MemberFreeSessionUsage usage = new MemberFreeSessionUsage();
            usage.setMemberId(MEMBER_ID);
            usage.setHasUsedFreeSession(false);
            when(freeUsageRepo.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(usage));
            when(freeUsageRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            boolean result = planService.useIncludedSession(MEMBER_ID);

            assertTrue(result);
        }

        @Test
        @DisplayName("Returns false when free session already used")
        void alreadyUsed_returnsFalse() {
            MemberFreeSessionUsage usage = new MemberFreeSessionUsage();
            usage.setMemberId(MEMBER_ID);
            usage.setHasUsedFreeSession(true);
            when(freeUsageRepo.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(usage));

            boolean result = planService.useIncludedSession(MEMBER_ID);

            assertFalse(result);
            verify(freeUsageRepo, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  useIncludedSession — SUBSCRIBED
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("useIncludedSession — subscribed member")
    class UseSession_Subscribed {

        @Test
        @DisplayName("Increments sessionsUsed and returns true when quota available")
        void quotaAvailable_incrementsAndReturnsTrue() {
            MemberSubscription sub = activeSub(5, 2);
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(sub));
            when(subRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            boolean result = planService.useIncludedSession(MEMBER_ID);

            assertTrue(result);
            verify(subRepo).save(argThat(s -> s.getSessionsUsed() == 3));
        }

        @Test
        @DisplayName("Returns false without saving when quota exhausted")
        void quotaExhausted_returnsFalse() {
            MemberSubscription sub = activeSub(5, 5);
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(sub));

            boolean result = planService.useIncludedSession(MEMBER_ID);

            assertFalse(result);
            verify(subRepo, never()).save(any());
        }

        @Test
        @DisplayName("Unlimited plan returns true without updating counter")
        void unlimitedPlan_returnsTrueWithoutSave() {
            MemberSubscription sub = activeSub(-1, 100);
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(sub));

            boolean result = planService.useIncludedSession(MEMBER_ID);

            assertTrue(result);
            verify(subRepo, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  restoreFreeSession — NON-SUBSCRIBED
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("restoreFreeSession — non-subscribed member")
    class Restore_NonSubscribed {

        @BeforeEach
        void noSubscription() {
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("Resets hasUsedFreeSession to false when member had used it")
        void usedBefore_resetsToFalse() {
            MemberFreeSessionUsage usage = new MemberFreeSessionUsage();
            usage.setMemberId(MEMBER_ID);
            usage.setHasUsedFreeSession(true);
            when(freeUsageRepo.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(usage));
            when(freeUsageRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            planService.restoreFreeSession(MEMBER_ID);

            verify(freeUsageRepo).save(argThat(u -> !u.isHasUsedFreeSession() && u.getUsedAt() == null));
        }

        @Test
        @DisplayName("No-op when no usage record exists")
        void noUsageRecord_noOp() {
            when(freeUsageRepo.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());

            planService.restoreFreeSession(MEMBER_ID);

            verify(freeUsageRepo, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  restoreFreeSession — SUBSCRIBED
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("restoreFreeSession — subscribed member")
    class Restore_Subscribed {

        @Test
        @DisplayName("Decrements sessionsUsed when sessions have been used")
        void hasSessions_decrements() {
            MemberSubscription sub = activeSub(5, 3);
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(sub));
            when(subRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            planService.restoreFreeSession(MEMBER_ID);

            verify(subRepo).save(argThat(s -> s.getSessionsUsed() == 2));
        }

        @Test
        @DisplayName("No-op when sessionsUsed is 0")
        void noSessionsUsed_noOp() {
            MemberSubscription sub = activeSub(5, 0);
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(sub));

            planService.restoreFreeSession(MEMBER_ID);

            verify(subRepo, never()).save(any());
        }

        @Test
        @DisplayName("No-op for unlimited plan")
        void unlimitedPlan_noOp() {
            MemberSubscription sub = activeSub(-1, 0);
            when(subRepo.findByMemberId(MEMBER_ID)).thenReturn(List.of(sub));

            planService.restoreFreeSession(MEMBER_ID);

            verify(subRepo, never()).save(any());
        }
    }
}
