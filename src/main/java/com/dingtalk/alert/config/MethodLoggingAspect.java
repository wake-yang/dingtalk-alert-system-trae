package com.dingtalk.alert.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 方法级别日志切面
 */
@Slf4j
@Aspect
@Component
public class MethodLoggingAspect {

    @Around("execution(* com.dingtalk.alert.controller.*.*(..)) || " +
            "execution(* com.dingtalk.alert.service.*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        try {
            log.debug("---> {} 开始执行", fullMethodName);
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            log.debug("<--- {} 执行完成，耗时: {}ms", fullMethodName, (endTime - startTime));
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("<--- {} 执行异常，耗时: {}ms，异常: {}", fullMethodName, (endTime - startTime), e.getMessage());
            throw e;
        }
    }
}