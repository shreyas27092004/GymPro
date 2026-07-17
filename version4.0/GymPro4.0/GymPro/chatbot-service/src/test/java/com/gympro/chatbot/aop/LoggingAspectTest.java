package com.gympro.chatbot.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoggingAspect}.
 * All dependencies are mocked — no Spring AOP proxying is required to exercise the advice logic directly.
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

    @Nested
    @DisplayName("logMethodExecution (@Around advice)")
    class LogMethodExecutionTests {

        @Test
        @DisplayName("Returns the proceeded result on successful execution")
        void logMethodExecution_success_returnsResult() throws Throwable {
            when(proceedingJoinPoint.getTarget()).thenReturn(new DummyTarget());
            when(proceedingJoinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("doSomething");
            when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"arg1", 42});
            when(proceedingJoinPoint.proceed()).thenReturn("success-result");

            Object result = loggingAspect.logMethodExecution(proceedingJoinPoint);

            assertThat(result).isEqualTo("success-result");
        }

        @Test
        @DisplayName("Handles null args array without throwing")
        void logMethodExecution_nullArgs_doesNotThrow() throws Throwable {
            when(proceedingJoinPoint.getTarget()).thenReturn(new DummyTarget());
            when(proceedingJoinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("doSomething");
            when(proceedingJoinPoint.getArgs()).thenReturn(null);
            when(proceedingJoinPoint.proceed()).thenReturn("ok");

            Object result = loggingAspect.logMethodExecution(proceedingJoinPoint);

            assertThat(result).isEqualTo("ok");
        }

        @Test
        @DisplayName("Handles empty args array without throwing")
        void logMethodExecution_emptyArgs_doesNotThrow() throws Throwable {
            when(proceedingJoinPoint.getTarget()).thenReturn(new DummyTarget());
            when(proceedingJoinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("doSomething");
            when(proceedingJoinPoint.getArgs()).thenReturn(new Object[0]);
            when(proceedingJoinPoint.proceed()).thenReturn("ok");

            Object result = loggingAspect.logMethodExecution(proceedingJoinPoint);

            assertThat(result).isEqualTo("ok");
        }

        @Test
        @DisplayName("Truncates long string arguments and null return value in logs")
        void logMethodExecution_longArgAndNullResult_truncatesAndSummarises() throws Throwable {
            String longArg = "x".repeat(200);
            when(proceedingJoinPoint.getTarget()).thenReturn(new DummyTarget());
            when(proceedingJoinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("doSomething");
            when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{longArg, null});
            when(proceedingJoinPoint.proceed()).thenReturn(null);

            Object result = loggingAspect.logMethodExecution(proceedingJoinPoint);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Truncates long return values in the exit log summary")
        void logMethodExecution_longResult_summarisesResult() throws Throwable {
            String longResult = "y".repeat(300);
            when(proceedingJoinPoint.getTarget()).thenReturn(new DummyTarget());
            when(proceedingJoinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("doSomething");
            when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"short"});
            when(proceedingJoinPoint.proceed()).thenReturn(longResult);

            Object result = loggingAspect.logMethodExecution(proceedingJoinPoint);

            assertThat(result).isEqualTo(longResult);
        }

        @Test
        @DisplayName("Re-throws exceptions raised by the target method after logging")
        void logMethodExecution_exceptionThrown_rethrows() throws Throwable {
            when(proceedingJoinPoint.getTarget()).thenReturn(new DummyTarget());
            when(proceedingJoinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("doSomething");
            when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"arg"});
            when(proceedingJoinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(() -> loggingAspect.logMethodExecution(proceedingJoinPoint))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("boom");
        }

        @Test
        @DisplayName("Handles an exception with a null message without throwing an unexpected error")
        void logMethodExecution_exceptionWithNullMessage_rethrows() throws Throwable {
            when(proceedingJoinPoint.getTarget()).thenReturn(new DummyTarget());
            when(proceedingJoinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("doSomething");
            when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});
            when(proceedingJoinPoint.proceed()).thenThrow(new RuntimeException());

            assertThatThrownBy(() -> loggingAspect.logMethodExecution(proceedingJoinPoint))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("logException (@AfterThrowing advice)")
    class LogExceptionTests {

        @Test
        @DisplayName("Logs exception details without throwing")
        void logException_doesNotThrow() {
            when(joinPoint.getTarget()).thenReturn(new DummyTarget());
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("doSomething");

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                    loggingAspect.logException(joinPoint, new IllegalArgumentException("bad input")));
        }

        @Test
        @DisplayName("Logs exception with a null message without throwing")
        void logException_nullMessage_doesNotThrow() {
            when(joinPoint.getTarget()).thenReturn(new DummyTarget());
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getName()).thenReturn("doSomething");

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                    loggingAspect.logException(joinPoint, new RuntimeException()));
        }
    }

    /** Simple dummy target class used to obtain a real Class for getSimpleName() calls. */
    private static class DummyTarget {
    }
}
