package com.backend.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class BackendApp {

    public static void main(String[] args) {
        SpringApplication.run(BackendApp.class, args);
    }
}
