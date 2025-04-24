package com.lgcns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class PopiConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PopiConfigServerApplication.class, args);
    }
}
