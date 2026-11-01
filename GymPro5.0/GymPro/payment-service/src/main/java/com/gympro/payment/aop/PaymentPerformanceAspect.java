package com.gympro.payment.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PaymentPerformanceAspect {

    private static final Logger log = LoggerFactory.getLogger(PaymentPerformanceAspect.class);
    private static final long PAYMENT_SLOW_MS  = 2000;
    private static final long RAZORPAY_SLOW_MS = 3000;

    @Pointcut("within(com.gympro.payment.service.PaymentService)")
    public void paymentService() {}

    @Pointcut("within(com.gympro.payment.service.RazorpayService)")
    public void razorpayService() {}

    @Around("paymentService()")
    public Object monitorPayment(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitor(joinPoint, PAYMENT_SLOW_MS);
    }

    @Around("razorpayService()")
    public Object monitorRazorpay(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitor(joinPoint, RAZORPAY_SLOW_MS);
    }

    private Object monitor(ProceedingJoinPoint joinPoint, long threshold) throws Throwable {
        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration > threshold) {
                log.warn("⚠️ SLOW PAYMENT METHOD: [{}.{}] took {}ms (threshold: {}ms)",
                    className, methodName, duration, threshold);
            } else {
                log.debug("⚡ [{}.{}] {}ms", className, methodName, duration);
            }
        }
    }
}