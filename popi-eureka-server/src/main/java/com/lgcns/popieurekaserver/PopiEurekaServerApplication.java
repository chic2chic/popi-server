package com.lgcns.popieurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class PopiEurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PopiEurekaServerApplication.class, args);
    }
}
