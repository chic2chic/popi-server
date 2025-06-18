package com.lgcns.aop.aspect;

import static com.lgcns.aop.util.LoggingUtil.calculateDuration;
import static com.lgcns.aop.util.LoggingUtil.getShortErrorMessage;

import com.lgcns.aop.util.LoggingUtil;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class KafkaProducerLoggingAspect {

    @Pointcut("execution(* com.lgcns..producer..*Producer.sendMessage(..))")
    public void allKafkaProducer() {}

    @Around("allKafkaProducer()")
    public Object logKafkaProducer(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = LoggingUtil.getMethodSignature(method);

        long start = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            log.error(
                    "[KAFKA-ERROR] Method: {}, Exception: {}, Message: {}, Duration: {}ms",
                    methodName,
                    e.getClass().getSimpleName(),
                    getShortErrorMessage(e.getMessage()),
                    calculateDuration(start));
            throw e;
        }
    }
}
