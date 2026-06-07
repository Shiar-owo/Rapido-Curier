package com.rapidocurier.paquetesservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PaquetesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaquetesServiceApplication.class, args);
    }

}
