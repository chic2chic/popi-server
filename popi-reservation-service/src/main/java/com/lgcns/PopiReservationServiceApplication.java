package com.lgcns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PopiReservationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PopiReservationServiceApplication.class, args);
    }
}
