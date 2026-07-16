package com.gympro.payment.aop;

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
class PaymentAuditAspectTest {

    private PaymentAuditAspect auditAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        auditAspect = new PaymentAuditAspect();
    }

    @Test
    @DisplayName("auditPaymentProcessed logs the request and result")
    void auditPaymentProcessed_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"request"});

        assertDoesNotThrow(() -> auditAspect.auditPaymentProcessed(joinPoint, "result"));
    }

    @Test
    @DisplayName("auditRefund logs paymentId, role and result")
    void auditRefund_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, "ADMIN"});

        assertDoesNotThrow(() -> auditAspect.auditRefund(joinPoint, "refundResult"));
    }

    @Test
    @DisplayName("auditOrderCreated logs amount and result")
    void auditOrderCreated_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{999.0, "desc"});

        assertDoesNotThrow(() -> auditAspect.auditOrderCreated(joinPoint, "orderResult"));
    }

    @Test
    @DisplayName("auditFailed logs the failed operation")
    void auditFailed_doesNotThrow() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("processPayment");

        assertDoesNotThrow(() -> auditAspect.auditFailed(joinPoint, new RuntimeException("db down")));
    }
}
