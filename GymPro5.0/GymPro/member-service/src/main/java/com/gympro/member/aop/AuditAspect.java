package com.gympro.member.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Pointcut("execution(* com.gympro.member.service.MemberService.createMember(..))")
    public void createMember() {}

    @Pointcut("execution(* com.gympro.member.service.MemberService.updateMember(..))")
    public void updateMember() {}

    @Pointcut("execution(* com.gympro.member.service.MemberService.deleteMember(..))")
    public void deleteMember() {}

    @After("createMember()")
    public void auditCreate(JoinPoint joinPoint) {
        auditLog.info("[AUDIT] MEMBER_CREATED | args={}", joinPoint.getArgs()[0]);
    }

    @After("updateMember()")
    public void auditUpdate(JoinPoint joinPoint) {
        auditLog.info("[AUDIT] MEMBER_UPDATED | memberId={}", joinPoint.getArgs()[0]);
    }

    @After("deleteMember()")
    public void auditDelete(JoinPoint joinPoint) {
        auditLog.info("[AUDIT] MEMBER_DEACTIVATED | memberId={}", joinPoint.getArgs()[0]);
    }
}