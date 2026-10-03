package com.empresax.sistema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SistemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SistemaApplication.class, args);
    }
}
