package com.gympro.auth.aop;

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

    @Pointcut("within(com.gympro.auth.controller..*)")
    public void controllerLayer() {}

    @Pointcut("within(com.gympro.auth.service..*)")
    public void serviceLayer() {}

    @Pointcut("within(com.gympro.auth.util..*)")
    public void utilLayer() {}

    @Pointcut("controllerLayer() || serviceLayer() || utilLayer()")
    public void applicationLayer() {}

    @Around("applicationLayer()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args     = maskPasswords(joinPoint.getArgs());

        log.info("→ [{}.{}] args={}", className, methodName, Arrays.toString(args));
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("← [{}.{}] {}ms → {}", className, methodName, duration, maskToken(String.valueOf(result)));
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("✗ [{}.{}] {} after {}ms → {}",
                className, methodName, ex.getClass().getSimpleName(), duration, ex.getMessage());
            throw ex;
        }
    }

    @AfterThrowing(pointcut = "applicationLayer()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        log.error("❌ Exception in [{}.{}]: [{}] {}",
            joinPoint.getTarget().getClass().getSimpleName(),
            joinPoint.getSignature().getName(),
            ex.getClass().getSimpleName(), ex.getMessage());
    }

    private Object[] maskPasswords(Object[] args) {
        if (args == null || args.length == 0) return args;
        Object[] masked = Arrays.copyOf(args, args.length);
        for (int i = 0; i < masked.length; i++) {
            if (masked[i] != null && masked[i].toString().contains("password=")) {
                masked[i] = masked[i].toString().replaceAll("password=[^,)]*", "password=***");
            }
        }
        return masked;
    }

    private String maskToken(String result) {
        if (result != null && result.contains("token=") && result.length() > 80) {
            return result.replaceAll("token=[A-Za-z0-9._-]{10,}", "token=***[JWT]***");
        }
        return result;
    }
}