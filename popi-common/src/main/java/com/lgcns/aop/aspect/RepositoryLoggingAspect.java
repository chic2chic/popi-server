package com.lgcns.aop.aspect;

import static com.lgcns.aop.util.LoggingUtil.calculateDuration;
import static com.lgcns.aop.util.LoggingUtil.getShortErrorMessage;

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

@Aspect
@Component
@Slf4j
public class RepositoryLoggingAspect {

    @Pointcut("execution(public * com.lgcns..repository..*.*(..))")
    public void allRepository() {}

    @Around("allRepository()")
    public Object logRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = LoggingUtil.getMethodSignature(method);

        long start = System.currentTimeMillis();

        try {
            return joinPoint.proceed();

        } catch (CustomException ce) {
            log.warn(
                    "[REPOSITORY-CUSTOM] Method: {}, Code: {}, Message: {}, Duration: {}ms",
                    methodName,
                    ce.getErrorCode(),
                    ce.getMessage(),
                    calculateDuration(start));
            throw ce;

        } catch (Exception e) {
            log.error(
                    "[REPOSITORY-ERROR] Method: {}, Exception: {}, Message: {}, Duration: {}ms",
                    methodName,
                    e.getClass().getSimpleName(),
                    getShortErrorMessage(e.getMessage()),
                    calculateDuration(start));
            throw e;
        }
    }
}
