package com.lgcns.aop.aspect;

import com.lgcns.aop.util.LoggingUtil;
import com.lgcns.error.exception.CustomException;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class EventHandlerLoggingAspect {

    @Pointcut("execution(public * com.lgcns..event..*.*(..))")
    public void allEventHandlers() {}

    @Around("allEventHandlers()")
    public Object logEventHandlers(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = LoggingUtil.getMethodSignature(method);

        String traceId = LoggingUtil.getTraceId();
        String memberId = LoggingUtil.getMemberId();

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            log.info(
                    "[EVENT] TraceId: {}, MemberId: {}, Method: {}, Duration: {}ms",
                    traceId,
                    memberId,
                    methodName,
                    duration);

            return result;
        } catch (CustomException ce) {
            log.warn(
                    "[EVENT-CUSTOM] TraceId: {}, MemberId: {}, Method: {}, Code: {}, Message: {}",
                    traceId,
                    memberId,
                    methodName,
                    ce.getErrorCode(),
                    ce.getMessage());
            throw ce;

        } catch (Exception e) {
            log.error(
                    "[EVENT-ERROR] TraceId: {}, MemberId: {}, Method: {}, Exception: {}, Message: {}",
                    traceId,
                    memberId,
                    methodName,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }
}
