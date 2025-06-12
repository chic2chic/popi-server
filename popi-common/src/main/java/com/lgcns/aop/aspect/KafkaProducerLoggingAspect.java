package com.lgcns.aop.aspect;

import com.lgcns.aop.util.LoggingUtil;
import java.lang.reflect.Method;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class KafkaProducerLoggingAspect {

    @Around("com.lgcns.aop.pointcut.LoggingPointCut.allKafkaProducer()")
    public Object logKafkaProducer(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = LoggingUtil.getMethodSignature(method);
        Map<String, Object> params = LoggingUtil.extractParams(method, joinPoint.getArgs());

        String traceId = LoggingUtil.getTraceId();
        String memberId = LoggingUtil.getMemberId();

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            log.info(
                    "[KAFKA] TraceId: {}, MemberId: {}, Method: {}, Payload: {}, Duration: {}ms",
                    traceId,
                    memberId,
                    methodName,
                    params,
                    duration);

            return result;
        } catch (Exception e) {
            log.error(
                    "[KAFKA-EXCEPTION] TraceId: {}, Method: {}, Exception: {}, Message: {}",
                    traceId,
                    methodName,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }
}
