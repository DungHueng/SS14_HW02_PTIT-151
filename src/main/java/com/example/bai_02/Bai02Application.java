package com.example.bai_02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Bai02Application {

    public static void main(String[] args) {
        SpringApplication.run(
                Bai02Application.class,
                args
        );
    }
}