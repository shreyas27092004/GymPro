package com.gympro.booking.aop;

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
class BookingAuditAspectTest {

    private BookingAuditAspect auditAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        auditAspect = new BookingAuditAspect();
    }

    @Test
    @DisplayName("auditCreated logs the request and result")
    void auditCreated_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"request"});

        assertDoesNotThrow(() -> auditAspect.auditCreated(joinPoint, "result"));
    }

    @Test
    @DisplayName("auditCancelled logs the booking id")
    void auditCancelled_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});

        assertDoesNotThrow(() -> auditAspect.auditCancelled(joinPoint, "Booking cancelled ✅"));
    }

    @Test
    @DisplayName("auditCompleted logs the booking id")
    void auditCompleted_doesNotThrow() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});

        assertDoesNotThrow(() -> auditAspect.auditCompleted(joinPoint, "result"));
    }

    @Test
    @DisplayName("auditFailed logs the failed operation")
    void auditFailed_doesNotThrow() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("createBooking");

        assertDoesNotThrow(() -> auditAspect.auditFailed(joinPoint, new RuntimeException("db down")));
    }
}
