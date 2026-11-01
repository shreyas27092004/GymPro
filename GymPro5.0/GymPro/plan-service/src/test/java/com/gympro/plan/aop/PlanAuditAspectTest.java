package com.gympro.plan.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanAuditAspectTest {

    private PlanAuditAspect auditAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        auditAspect = new PlanAuditAspect();
    }

    @Test
    @DisplayName("auditPlanCreated logs the created plan result")
    void auditPlanCreated_doesNotThrow() {
        assertDoesNotThrow(() -> auditAspect.auditPlanCreated(joinPoint, "planResult"));
    }

    @Test
    @DisplayName("auditPlanDeactivated logs the plan id")
    void auditPlanDeactivated_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});

        assertDoesNotThrow(() -> auditAspect.auditPlanDeactivated(joinPoint));
    }

    @Test
    @DisplayName("auditSubscription logs memberId, planId and result")
    void auditSubscription_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "member@x.com", 5L});

        assertDoesNotThrow(() -> auditAspect.auditSubscription(joinPoint, "subResult"));
    }

    @Test
    @DisplayName("auditCancellation logs the subscription id")
    void auditCancellation_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{7L});

        assertDoesNotThrow(() -> auditAspect.auditCancellation(joinPoint));
    }

    @Test
    @DisplayName("auditFailure logs the failed operation")
    void auditFailure_doesNotThrow() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("createPlan");

        assertDoesNotThrow(() -> auditAspect.auditFailure(joinPoint, new RuntimeException("db down")));
    }
}
