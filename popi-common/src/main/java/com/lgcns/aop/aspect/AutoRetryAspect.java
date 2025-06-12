package com.lgcns.aop.aspect;

import com.lgcns.aop.annotation.AutoRetry;
import com.lgcns.aop.util.LoggingUtil;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class AutoRetryAspect {

    @Around("@annotation(autoRetry)")
    public Object doAutoRetry(ProceedingJoinPoint joinPoint, AutoRetry autoRetry) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        int maxRetry = autoRetry.value();
        String traceId = LoggingUtil.getTraceId();

        Exception exceptionHolder = null;

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            exceptionHolder = e;
            log.warn(
                    "[RETRY] First attempt failed. traceId={}, method={}, retry={}",
                    traceId,
                    methodName,
                    maxRetry);
        }

        for (int i = 1; i <= maxRetry; i++) {
            try {
                log.info(
                        "[RETRY] Retrying... traceId={}, method={}, attempt={}/{}",
                        traceId,
                        methodName,
                        i,
                        maxRetry);
                return joinPoint.proceed();
            } catch (Exception e) {
                exceptionHolder = e;

                long delay = Math.min((long) Math.pow(2, i) * 100L, 1000L);
                Thread.sleep(delay);
            }
        }

        throw Objects.requireNonNull(exceptionHolder);
    }
}
