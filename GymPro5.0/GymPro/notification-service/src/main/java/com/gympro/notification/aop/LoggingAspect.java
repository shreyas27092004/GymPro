package com.gympro.notification.aop;

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

    @Pointcut("within(com.gympro.notification.controller..*)")
    public void controllerLayer() {}

    @Pointcut("within(com.gympro.notification.service..*)")
    public void serviceLayer() {}

    @Pointcut("controllerLayer() || serviceLayer()")
    public void applicationLayer() {}

    @Around("applicationLayer()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.info("→ [{}.{}] args={}", className, methodName,
            Arrays.toString(truncateArgs(joinPoint.getArgs())));
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("← [{}.{}] {}ms → {}", className, methodName,
                System.currentTimeMillis() - start, result);
            return result;
        } catch (Throwable ex) {
            log.error("✗ EMAIL SEND FAILED [{}.{}] {} after {}ms → {}",
                className, methodName, ex.getClass().getSimpleName(),
                System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }

    @AfterThrowing(pointcut = "applicationLayer()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        log.error("❌ EMAIL SEND FAILED [{}.{}] [{}] {}",
            joinPoint.getTarget().getClass().getSimpleName(),
            joinPoint.getSignature().getName(),
            ex.getClass().getSimpleName(), ex.getMessage());
    }

    private Object[] truncateArgs(Object[] args) {
        if (args == null) return args;
        Object[] safe = Arrays.copyOf(args, args.length);
        for (int i = 0; i < safe.length; i++) {
            if (safe[i] instanceof String s && s.length() > 80) {
                safe[i] = s.substring(0, 80) + "...[truncated]";
            }
        }
        return safe;
    }
}