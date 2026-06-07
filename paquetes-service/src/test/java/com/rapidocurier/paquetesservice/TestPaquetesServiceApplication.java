package com.rapidocurier.paquetesservice;

import org.springframework.boot.SpringApplication;

public class TestPaquetesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(PaquetesServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
