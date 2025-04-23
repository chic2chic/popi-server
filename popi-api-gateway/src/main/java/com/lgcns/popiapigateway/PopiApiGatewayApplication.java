package com.lgcns.popiapigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PopiApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PopiApiGatewayApplication.class, args);
    }

}
