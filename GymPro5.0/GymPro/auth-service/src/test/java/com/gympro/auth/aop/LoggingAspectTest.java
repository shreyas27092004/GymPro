package com.gympro.auth.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
        when(signature.getName()).thenReturn("login");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"user@x.com"});
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        Object result = loggingAspect.logMethodExecution(proceedingJoinPoint);

        assertEquals("result", result);
    }

    @Test
    @DisplayName("Masks password= in the logged arguments without altering the returned result")
    void logMethodExecution_maskPasswords_masksSensitiveArg() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("register");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"User(email=x@y.com, password=Secret123)"});
        when(proceedingJoinPoint.proceed()).thenReturn("ok");

        Object result = loggingAspect.logMethodExecution(proceedingJoinPoint);

        assertEquals("ok", result);
    }

    @Test
    @DisplayName("Handles null args array without throwing")
    void logMethodExecution_nullArgs_doesNotThrow() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("test");
        when(proceedingJoinPoint.getArgs()).thenReturn(null);
        when(proceedingJoinPoint.proceed()).thenReturn("ok");

        assertDoesNotThrow(() -> loggingAspect.logMethodExecution(proceedingJoinPoint));
    }

    @Test
    @DisplayName("Masks a long JWT-looking token= in the logged result")
    void logMethodExecution_maskToken_masksLongToken() throws Throwable {
        String fakeJwt = "token=" + "a".repeat(100);
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("login");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"req"});
        when(proceedingJoinPoint.proceed()).thenReturn(fakeJwt);

        Object result = loggingAspect.logMethodExecution(proceedingJoinPoint);

        assertEquals(fakeJwt, result);
    }

    @Test
    @DisplayName("Does not attempt to mask a short result containing token=")
    void logMethodExecution_shortTokenResult_notMasked() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("login");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"req"});
        when(proceedingJoinPoint.proceed()).thenReturn("token=abc");

        Object result = loggingAspect.logMethodExecution(proceedingJoinPoint);

        assertEquals("token=abc", result);
    }

    @Test
    @DisplayName("Re-throws exceptions raised by the target method after logging")
    void logMethodExecution_exception_rethrows() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("login");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"req"});
        when(proceedingJoinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class,
                () -> loggingAspect.logMethodExecution(proceedingJoinPoint));
    }

    @Test
    @DisplayName("logException logs details without throwing")
    void logException_doesNotThrow() {
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("login");

        assertDoesNotThrow(() -> loggingAspect.logException(joinPoint, new RuntimeException("fail")));
    }
}
