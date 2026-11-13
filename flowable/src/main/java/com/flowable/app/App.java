package com.flowable.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entrypoint of the embedded Flowable engine.
 *
 * <p>Users and groups are created by
 * {@link com.flowable.app.config.IdentitySeeder}, which runs on every profile.
 */
@SpringBootApplication(scanBasePackages = "com.flowable")
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
