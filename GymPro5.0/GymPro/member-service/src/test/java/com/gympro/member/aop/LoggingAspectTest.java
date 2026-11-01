package com.gympro.member.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoggingAspect}.
 * All AspectJ types are mocked so the advice logic runs without needing a live proxy.
 */
@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    private LoggingAspect loggingAspect;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        loggingAspect = new LoggingAspect();
    }

    @Test
    @DisplayName("Returns the proceeded result on successful execution")
    void logMethodExecution_success_returnsResult() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("createMember");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"arg"});
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        Object result = loggingAspect.logMethodExecution(proceedingJoinPoint);

        assertEquals("result", result);
    }

    @Test
    @DisplayName("Re-throws exceptions raised by the target method after logging")
    void logMethodExecution_exception_rethrows() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("deleteMember");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{1L});
        when(proceedingJoinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class,
                () -> loggingAspect.logMethodExecution(proceedingJoinPoint));
    }

    @Test
    @DisplayName("logException logs details without throwing")
    void logException_doesNotThrow() {
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("updateMember");

        assertDoesNotThrow(() -> loggingAspect.logException(joinPoint, new RuntimeException("fail")));
    }
}
