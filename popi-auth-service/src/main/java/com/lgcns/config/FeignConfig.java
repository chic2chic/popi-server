package com.lgcns.config;

import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.lgcns.client")
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header(
                    "Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            if (requestTemplate.body() == null) {
                requestTemplate.body("");
            }
        };
    }
}
