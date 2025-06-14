package com.lgcns.aop.aspect;

import static com.lgcns.aop.util.LoggingUtil.calculateDuration;
import static com.lgcns.aop.util.LoggingUtil.getShortErrorMessage;

import com.lgcns.aop.util.LoggingUtil;
import com.lgcns.error.exception.CustomException;
import com.lgcns.error.exception.ErrorCode;
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
public class ServiceLoggingAspect {

    @Pointcut("execution(public * com.lgcns..service..*.*(..))")
    public void allService() {}

    @Around("allService()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = LoggingUtil.getMethodSignature(method);

        long start = System.currentTimeMillis();

        log.info("[SERVICE] Method: {}", methodName);

        try {
            Object result = joinPoint.proceed();
            log.info("[SERVICE] Method: {}, Duration: {}ms", methodName, calculateDuration(start));
            return result;
        } catch (CustomException customException) {
            ErrorCode errorCode = customException.getErrorCode();
            log.info(
                    "[CustomException] Method: {}, Code: {}, Message: {}, Duration: {}ms",
                    methodName,
                    errorCode,
                    customException.getMessage(),
                    calculateDuration(start));

            throw customException;
        } catch (Exception e) {
            log.error(
                    "[UnhandledException] Method: {}, Exception: {}, Message: {}, Duration: {}ms",
                    methodName,
                    e.getClass().getSimpleName(),
                    getShortErrorMessage(e.getMessage()),
                    calculateDuration(start));
            throw e;
        }
    }
}
