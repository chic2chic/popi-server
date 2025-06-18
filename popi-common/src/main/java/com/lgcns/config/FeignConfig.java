package com.lgcns.config;

import com.lgcns.aop.util.LoggingUtil;
import com.lgcns.error.feign.FeignErrorDecoder;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients("com.lgcns")
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return template -> {
            String traceId = LoggingUtil.getTraceId();
            String memberId = LoggingUtil.getMemberId();

            if (traceId != null) template.header("trace-id", traceId);
            if (memberId != null) template.header("member-id", memberId);
        };
    }
}
