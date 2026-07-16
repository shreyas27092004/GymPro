package com.gympro.plan.exception;

import com.gympro.plan.entity.MembershipPlan;
import com.gympro.plan.entity.MemberSubscription;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ModelAndExceptionTest {

    // ── MembershipPlan entity ──────────────────────────────
	@Test
	void membershipPlan_AllArgsConstructor_ShouldWork() {
	    MembershipPlan plan = new MembershipPlan(1L, "Gold Monthly", "Full access",
	            "MONTHLY", 999.0, 30, true, 0);  // ← added sessionsIncluded = 0

	    assertEquals(1L, plan.getId());
	    assertEquals("Gold Monthly", plan.getPlanName());
	    assertEquals("Full access", plan.getDescription());
	    assertEquals("MONTHLY", plan.getDurationType());
	    assertEquals(999.0, plan.getPrice());
	    assertEquals(30, plan.getDurationDays());
	    assertTrue(plan.isActive());
	    assertEquals(0, plan.getSessionsIncluded());  // ← added
	}

	@Test
	void membershipPlan_Equals_ShouldWork() {
	    MembershipPlan plan1 = new MembershipPlan(1L, "Gold", "Desc", "MONTHLY", 999.0, 30, true, 0);  // ← added 0
	    MembershipPlan plan2 = new MembershipPlan(1L, "Gold", "Desc", "MONTHLY", 999.0, 30, true, 0);  // ← added 0
	    assertEquals(plan1, plan2);
	}

	@Test
	void membershipPlan_HashCode_ShouldBeConsistent() {
	    MembershipPlan plan = new MembershipPlan(1L, "Gold", "Desc", "MONTHLY", 999.0, 30, true, 0);  // ← added 0
	    assertEquals(plan.hashCode(), plan.hashCode());
	}

	@Test
	void memberSubscription_AllArgsConstructor_ShouldWork() {
	    LocalDate start = LocalDate.now();
	    LocalDate end = start.plusDays(30);

	    MemberSubscription sub = new MemberSubscription(10L, 5L, "member@gym.com",
	            1L, "Gold Monthly", start, end, "ACTIVE", 0);  // ← added sessionsUsed = 0

	    assertEquals(10L, sub.getId());
	    assertEquals(5L, sub.getMemberId());
	    assertEquals("member@gym.com", sub.getMemberEmail());
	    assertEquals(1L, sub.getPlanId());
	    assertEquals("Gold Monthly", sub.getPlanName());
	    assertEquals(start, sub.getStartDate());
	    assertEquals(end, sub.getEndDate());
	    assertEquals("ACTIVE", sub.getStatus());
	    assertEquals(0, sub.getSessionsUsed());  // ← added
	}

	@Test
	void memberSubscription_Equals_ShouldWork() {
	    LocalDate start = LocalDate.now();
	    MemberSubscription s1 = new MemberSubscription(1L, 2L, "a@b.com", 3L, "Plan", start, start.plusDays(30), "ACTIVE", 0);  // ← added 0
	    MemberSubscription s2 = new MemberSubscription(1L, 2L, "a@b.com", 3L, "Plan", start, start.plusDays(30), "ACTIVE", 0);  // ← added 0
	    assertEquals(s1, s2);
	}
}
