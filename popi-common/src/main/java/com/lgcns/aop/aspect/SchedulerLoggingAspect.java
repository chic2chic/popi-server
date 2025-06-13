package com.lgcns.aop.aspect;

import com.lgcns.aop.util.LoggingUtil;
import com.lgcns.error.exception.CustomException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
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
public class SchedulerLoggingAspect {

    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void allScheduledJobs() {}

    @Around("allScheduledJobs()")
    public Object logScheduler(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = LoggingUtil.getMethodSignature(method);
        Map<String, Object> params = LoggingUtil.extractParams(method, joinPoint.getArgs());

        String traceId = UUID.randomUUID().toString();
        LoggingUtil.setTraceId(traceId);

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            log.info(
                    "[SCHEDULER] TraceId: {}, Method: {}, Duration: {}ms",
                    traceId,
                    methodName,
                    duration);
            return result;

        } catch (CustomException ce) {
            log.info(
                    "[CustomException] TraceId: {}, Method: {}, Code: {}, Message: {}",
                    traceId,
                    methodName,
                    ce.getErrorCode(),
                    ce.getMessage());
            throw ce;

        } catch (Exception e) {
            log.error(
                    "[UnhandledException] TraceId: {}, Method: {}, Exception: {}, Message: {}",
                    traceId,
                    methodName,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            throw e;
        } finally {
            LoggingUtil.clearMDC();
        }
    }
}
