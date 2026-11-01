package com.gympro.auth.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceAspectTest {

    private PerformanceAspect performanceAspect;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        performanceAspect = new PerformanceAspect();
    }

    @Test
    @DisplayName("Returns the proceeded result and logs at debug level when under the slow threshold")
    void monitorPerformance_fastMethod_returnsResult() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("login");
        when(proceedingJoinPoint.proceed()).thenReturn("fast-result");

        Object result = performanceAspect.monitorPerformance(proceedingJoinPoint);

        assertEquals("fast-result", result);
    }

    @Test
    @DisplayName("Logs a slow-method warning when execution exceeds the threshold, but still returns the result")
    void monitorPerformance_slowMethod_logsWarningAndReturnsResult() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("register");
        when(proceedingJoinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(600);
            return "slow-result";
        });

        Object result = performanceAspect.monitorPerformance(proceedingJoinPoint);

        assertEquals("slow-result", result);
    }

    @Test
    @DisplayName("Re-throws the exception even when the method itself fails, still recording duration in the finally block")
    void monitorPerformance_exceptionThrown_rethrows() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("login");
        when(proceedingJoinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class,
                () -> performanceAspect.monitorPerformance(proceedingJoinPoint));
    }
}
