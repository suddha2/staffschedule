package com.midco.rota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RotaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RotaServiceApplication.class, args);
    }
}	