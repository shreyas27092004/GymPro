package com.gympro.trainer.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TrainerAuditAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Pointcut("execution(* com.gympro.trainer.service.TrainerService.createTrainer(..))")
    public void createTrainer() {}

    @Pointcut("execution(* com.gympro.trainer.service.TrainerService.deleteTrainer(..))")
    public void deleteTrainer() {}

    @Pointcut("execution(* com.gympro.trainer.service.TrainerService.addSchedule(..))")
    public void addSchedule() {}

    @Pointcut("execution(* com.gympro.trainer.service.TrainerService.markSlotBooked(..))")
    public void markSlotBooked() {}

    @AfterReturning(pointcut = "createTrainer()", returning = "result")
    public void auditCreated(JoinPoint joinPoint, Object result) {
        auditLog.info("[AUDIT] TRAINER_CREATED | trainer={}", result);
    }

    @AfterReturning(pointcut = "deleteTrainer()")
    public void auditDeleted(JoinPoint joinPoint) {
        auditLog.info("[AUDIT] TRAINER_DEACTIVATED | trainerId={}", joinPoint.getArgs()[0]);
    }

    @AfterReturning(pointcut = "addSchedule()", returning = "result")
    public void auditScheduleAdded(JoinPoint joinPoint, Object result) {
        auditLog.info("[AUDIT] SCHEDULE_ADDED | schedule={}", result);
    }

    @AfterReturning(pointcut = "markSlotBooked()")
    public void auditSlotBooked(JoinPoint joinPoint) {
        auditLog.info("[AUDIT] SCHEDULE_SLOT_BOOKED | scheduleId={}", joinPoint.getArgs()[0]);
    }

    @AfterThrowing(pointcut = "createTrainer() || deleteTrainer() || addSchedule() || markSlotBooked()", throwing = "ex")
    public void auditFailed(JoinPoint joinPoint, Throwable ex) {
        auditLog.warn("[AUDIT] TRAINER_OPERATION_FAILED | method={} | error={}",
            joinPoint.getSignature().getName(), ex.getMessage());
    }
}