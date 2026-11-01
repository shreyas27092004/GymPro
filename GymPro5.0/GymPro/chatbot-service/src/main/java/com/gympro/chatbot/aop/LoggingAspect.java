package com.gympro.chatbot.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP logging aspect for the chatbot-service.
 *
 * Mirrors the LoggingAspect pattern used in auth-service so all GymPro
 * microservices have consistent structured logging.
 *
 * Logs:
 *  → method entry  : class, method name, arguments (messages truncated for brevity)
 *  ← method exit   : duration in ms, return value summary
 *  ✗ exception     : exception type + message (on throw)
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /** Max characters of a single arg to log (prevents huge message bodies in logs) */
    private static final int MAX_ARG_LENGTH = 120;

    @Pointcut("within(com.gympro.chatbot.controller..*)")
    public void controllerLayer() {}

    @Pointcut("within(com.gympro.chatbot.service..*)")
    public void serviceLayer() {}

    @Pointcut("controllerLayer() || serviceLayer()")
    public void applicationLayer() {}

    /**
     * Wraps every controller and service method to log entry, exit, and duration.
     */
    @Around("applicationLayer()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args     = truncateArgs(joinPoint.getArgs());

        log.info("→ [{}.{}] args={}", className, methodName, Arrays.toString(args));

        long start = System.currentTimeMillis();
        try {
            Object result   = joinPoint.proceed();
            long   duration = System.currentTimeMillis() - start;
            log.info("← [{}.{}] {}ms → {}",
                    className, methodName, duration, summarise(result));
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("✗ [{}.{}] {} after {}ms → {}",
                    className, methodName,
                    ex.getClass().getSimpleName(), duration, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Extra logging for uncaught exceptions (complements the @Around advice).
     */
    @AfterThrowing(pointcut = "applicationLayer()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        log.error("❌ Exception in [{}.{}]: [{}] {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Truncates each argument string to MAX_ARG_LENGTH characters. */
    private Object[] truncateArgs(Object[] args) {
        if (args == null || args.length == 0) return args;
        Object[] trimmed = Arrays.copyOf(args, args.length);
        for (int i = 0; i < trimmed.length; i++) {
            if (trimmed[i] != null) {
                String str = trimmed[i].toString();
                if (str.length() > MAX_ARG_LENGTH) {
                    trimmed[i] = str.substring(0, MAX_ARG_LENGTH) + "…";
                }
            }
        }
        return trimmed;
    }

    /** Produces a short summary of a return value for logging. */
    private String summarise(Object result) {
        if (result == null) return "null";
        String str = result.toString();
        return str.length() > MAX_ARG_LENGTH ? str.substring(0, MAX_ARG_LENGTH) + "…" : str;
    }
}
