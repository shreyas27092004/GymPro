package com.gympro.trainer.aop;

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
class TrainerAuditAspectTest {

    private TrainerAuditAspect auditAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        auditAspect = new TrainerAuditAspect();
    }

    @Test
    @DisplayName("auditCreated logs the created trainer result")
    void auditCreated_doesNotThrow() {
        assertDoesNotThrow(() -> auditAspect.auditCreated(joinPoint, "trainerResult"));
    }

    @Test
    @DisplayName("auditDeleted logs the deleted trainer id")
    void auditDeleted_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});

        assertDoesNotThrow(() -> auditAspect.auditDeleted(joinPoint));
    }

    @Test
    @DisplayName("auditScheduleAdded logs the added schedule result")
    void auditScheduleAdded_doesNotThrow() {
        assertDoesNotThrow(() -> auditAspect.auditScheduleAdded(joinPoint, "scheduleResult"));
    }

    @Test
    @DisplayName("auditSlotBooked logs the booked schedule id")
    void auditSlotBooked_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{5L});

        assertDoesNotThrow(() -> auditAspect.auditSlotBooked(joinPoint));
    }

    @Test
    @DisplayName("auditFailed logs the failed operation without throwing")
    void auditFailed_doesNotThrow() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("createTrainer");

        assertDoesNotThrow(() -> auditAspect.auditFailed(joinPoint, new RuntimeException("db down")));
    }

    @Test
    @DisplayName("auditFailed handles a null exception message without throwing")
    void auditFailed_nullMessage_doesNotThrow() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("deleteTrainer");

        assertDoesNotThrow(() -> auditAspect.auditFailed(joinPoint, new RuntimeException()));
    }
}
