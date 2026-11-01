package com.gympro.plan.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PlanAuditAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Pointcut("execution(* com.gympro.plan.service.PlanService.createPlan(..))")
    public void createPlan() {}

    @Pointcut("execution(* com.gympro.plan.service.PlanService.deactivatePlan(..))")
    public void deactivatePlan() {}

    @Pointcut("execution(* com.gympro.plan.service.PlanService.subscribe(..))")
    public void subscribe() {}

    @Pointcut("execution(* com.gympro.plan.service.PlanService.cancelSubscription(..))")
    public void cancelSubscription() {}

    @AfterReturning(pointcut = "createPlan()", returning = "result")
    public void auditPlanCreated(JoinPoint joinPoint, Object result) {
        auditLog.info("[AUDIT] PLAN_CREATED | plan={}", result);
    }

    @AfterReturning(pointcut = "deactivatePlan()")
    public void auditPlanDeactivated(JoinPoint joinPoint) {
        auditLog.info("[AUDIT] PLAN_DEACTIVATED | planId={}", joinPoint.getArgs()[0]);
    }

    @AfterReturning(pointcut = "subscribe()", returning = "result")
    public void auditSubscription(JoinPoint joinPoint, Object result) {
        auditLog.info("[AUDIT] MEMBER_SUBSCRIBED | memberId={} | planId={} | result={}",
            joinPoint.getArgs()[0], joinPoint.getArgs()[2], result);
    }

    @AfterReturning(pointcut = "cancelSubscription()")
    public void auditCancellation(JoinPoint joinPoint) {
        auditLog.info("[AUDIT] SUBSCRIPTION_CANCELLED | subId={}", joinPoint.getArgs()[0]);
    }

    @AfterThrowing(pointcut = "createPlan() || deactivatePlan() || subscribe() || cancelSubscription()", throwing = "ex")
    public void auditFailure(JoinPoint joinPoint, Throwable ex) {
        auditLog.warn("[AUDIT] PLAN_OPERATION_FAILED | method={} | error={}",
            joinPoint.getSignature().getName(), ex.getMessage());
    }
}