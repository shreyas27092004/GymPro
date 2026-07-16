package com.gympro.booking.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BookingAuditAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Pointcut("execution(* com.gympro.booking.service.BookingService.createBooking(..))")
    public void createBooking() {}

    @Pointcut("execution(* com.gympro.booking.service.BookingService.cancelBooking(..))")
    public void cancelBooking() {}

    @Pointcut("execution(* com.gympro.booking.service.BookingService.completeBooking(..))")
    public void completeBooking() {}

    @AfterReturning(pointcut = "createBooking()", returning = "result")
    public void auditCreated(JoinPoint joinPoint, Object result) {
        auditLog.info("[AUDIT] BOOKING_CREATED | request={} | result={}", joinPoint.getArgs()[0], result);
    }

    @AfterReturning(pointcut = "cancelBooking()", returning = "result")
    public void auditCancelled(JoinPoint joinPoint, Object result) {
        auditLog.info("[AUDIT] BOOKING_CANCELLED | bookingId={}", joinPoint.getArgs()[0]);
    }

    @AfterReturning(pointcut = "completeBooking()", returning = "result")
    public void auditCompleted(JoinPoint joinPoint, Object result) {
        auditLog.info("[AUDIT] BOOKING_COMPLETED | bookingId={}", joinPoint.getArgs()[0]);
    }

    @AfterThrowing(pointcut = "createBooking() || cancelBooking() || completeBooking()", throwing = "ex")
    public void auditFailed(JoinPoint joinPoint, Throwable ex) {
        auditLog.warn("[AUDIT] BOOKING_OPERATION_FAILED | method={} | error={}",
            joinPoint.getSignature().getName(), ex.getMessage());
    }
}