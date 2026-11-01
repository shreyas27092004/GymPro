package com.gympro.auth.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAspect.class);
    private static final long SLOW_THRESHOLD_MS = 500;

    @Pointcut("within(com.gympro.auth.service..*)")
    public void serviceLayer() {}

    @Around("serviceLayer()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration > SLOW_THRESHOLD_MS) {
                log.warn("⚠️ SLOW METHOD: [{}.{}] took {}ms (threshold: {}ms)",
                    className, methodName, duration, SLOW_THRESHOLD_MS);
            } else {
                log.debug("⚡ [{}.{}] completed in {}ms", className, methodName, duration);
            }
        }
    }
}