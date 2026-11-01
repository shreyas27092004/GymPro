package com.gympro.plan.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("within(com.gympro.plan.controller..*)")
    public void controllerLayer() {}

    @Pointcut("within(com.gympro.plan.service..*)")
    public void serviceLayer() {}

    @Pointcut("controllerLayer() || serviceLayer()")
    public void applicationLayer() {}

    @Around("applicationLayer()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.info("→ [{}.{}] args={}", className, methodName, Arrays.toString(joinPoint.getArgs()));
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("← [{}.{}] {}ms → {}", className, methodName,
                System.currentTimeMillis() - start, result);
            return result;
        } catch (Throwable ex) {
            log.error("✗ [{}.{}] {} after {}ms → {}",
                className, methodName, ex.getClass().getSimpleName(),
                System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }

    @AfterThrowing(pointcut = "applicationLayer()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        log.error("❌ [{}.{}] [{}] {}",
            joinPoint.getTarget().getClass().getSimpleName(),
            joinPoint.getSignature().getName(),
            ex.getClass().getSimpleName(), ex.getMessage());
    }
}