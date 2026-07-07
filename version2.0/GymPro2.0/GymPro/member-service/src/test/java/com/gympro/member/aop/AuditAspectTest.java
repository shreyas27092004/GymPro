package com.gympro.member.aop;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditAspect}.
 * Verifies each @After advice method executes without throwing, given a mocked JoinPoint.
 */
@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    private AuditAspect auditAspect;

    @Mock
    private JoinPoint joinPoint;

    @BeforeEach
    void setUp() {
        auditAspect = new AuditAspect();
    }

    @Test
    @DisplayName("auditCreate logs the created member argument")
    void auditCreate_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"newMemberPayload"});

        assertDoesNotThrow(() -> auditAspect.auditCreate(joinPoint));
    }

    @Test
    @DisplayName("auditUpdate logs the updated member id")
    void auditUpdate_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});

        assertDoesNotThrow(() -> auditAspect.auditUpdate(joinPoint));
    }

    @Test
    @DisplayName("auditDelete logs the deactivated member id")
    void auditDelete_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{2L});

        assertDoesNotThrow(() -> auditAspect.auditDelete(joinPoint));
    }
}
