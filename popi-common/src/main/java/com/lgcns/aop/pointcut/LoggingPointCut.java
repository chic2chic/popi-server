package com.lgcns.aop.pointcut;

import org.aspectj.lang.annotation.Pointcut;

public class LoggingPointCut {

    @Pointcut("execution(public * com.lgcns..service..*.*(..))")
    public void allService() {}

    @Pointcut("execution(public * com.lgcns..repository..*.*(..))")
    public void allRepository() {}

    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void allScheduledJobs() {}

    @Pointcut("execution(* com.lgcns..client..*.*(..))")
    public void allFeignClient() {}

    @Pointcut("execution(* com.lgcns..producer..*Producer.sendMessage(..))")
    public void allKafkaProducer() {}

    @Pointcut("execution(public * com.lgcns..event..*.*(..))")
    public void allEventHandlers() {}
}
