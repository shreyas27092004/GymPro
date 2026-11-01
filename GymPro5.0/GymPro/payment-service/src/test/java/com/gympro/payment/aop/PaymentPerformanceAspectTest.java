package com.gympro.payment.aop;

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
class PaymentPerformanceAspectTest {

    private PaymentPerformanceAspect performanceAspect;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        performanceAspect = new PaymentPerformanceAspect();
    }

    @Test
    @DisplayName("monitorPayment returns the proceeded result and logs at debug level under the threshold")
    void monitorPayment_fastMethod_returnsResult() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("processPayment");
        when(proceedingJoinPoint.proceed()).thenReturn("payment-result");

        Object result = performanceAspect.monitorPayment(proceedingJoinPoint);

        assertEquals("payment-result", result);
    }

    @Test
    @DisplayName("monitorRazorpay returns the proceeded result and logs at debug level under the threshold")
    void monitorRazorpay_fastMethod_returnsResult() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("createOrder");
        when(proceedingJoinPoint.proceed()).thenReturn("order-result");

        Object result = performanceAspect.monitorRazorpay(proceedingJoinPoint);

        assertEquals("order-result", result);
    }

    @Test
    @DisplayName("Re-throws the exception from the target method, still recording duration in the finally block")
    void monitorPayment_exceptionThrown_rethrows() throws Throwable {
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("refund");
        when(proceedingJoinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class,
                () -> performanceAspect.monitorPayment(proceedingJoinPoint));
    }
}
