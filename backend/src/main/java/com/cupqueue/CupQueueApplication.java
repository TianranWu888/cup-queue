package com.cupqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstraps the CupQueue backend application.
 */
@SpringBootApplication
public class CupQueueApplication {

    /**
     * Creates the application bootstrap configuration.
     */
    public CupQueueApplication() {
    }

    /**
     * Starts the Spring application context and embedded web server.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(CupQueueApplication.class, args);
    }

}
