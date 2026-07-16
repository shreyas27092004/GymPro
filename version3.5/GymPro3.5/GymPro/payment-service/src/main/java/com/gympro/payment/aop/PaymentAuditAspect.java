package com.gympro.payment.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PaymentAuditAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("PAYMENT_AUDIT");

    @Pointcut("execution(* com.gympro.payment.service.PaymentService.processPayment(..))")
    public void processPayment() {}

    @Pointcut("execution(* com.gympro.payment.service.PaymentService.refund(..))")
    public void refund() {}

    @Pointcut("execution(* com.gympro.payment.service.PaymentService.createRazorpayOrder(..))")
    public void createRazorpayOrder() {}

    @AfterReturning(pointcut = "processPayment()", returning = "result")
    public void auditPaymentProcessed(JoinPoint joinPoint, Object result) {
        auditLog.info("[PAYMENT_AUDIT] PAYMENT_PROCESSED | request={} | result={}",
            joinPoint.getArgs()[0], result);
    }

    @AfterReturning(pointcut = "refund()", returning = "result")
    public void auditRefund(JoinPoint joinPoint, Object result) {
        auditLog.info("[PAYMENT_AUDIT] REFUND_ISSUED | paymentId={} | role={} | result={}",
            joinPoint.getArgs()[0], joinPoint.getArgs()[1], result);
    }

    @AfterReturning(pointcut = "createRazorpayOrder()", returning = "result")
    public void auditOrderCreated(JoinPoint joinPoint, Object result) {
        auditLog.info("[PAYMENT_AUDIT] RAZORPAY_ORDER_CREATED | amount=₹{} | order={}",
            joinPoint.getArgs()[0], result);
    }

    @AfterThrowing(pointcut = "processPayment() || refund() || createRazorpayOrder()", throwing = "ex")
    public void auditFailed(JoinPoint joinPoint, Throwable ex) {
        auditLog.error("[PAYMENT_AUDIT] PAYMENT_OPERATION_FAILED | method={} | error=[{}] {}",
            joinPoint.getSignature().getName(), ex.getClass().getSimpleName(), ex.getMessage());
    }
}