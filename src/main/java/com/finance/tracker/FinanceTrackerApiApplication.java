package com.finance.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Finance Tracker API
 * 
 * @SpringBootApplication is a convenience annotation that combines:
 * - @Configuration: Tags the class as a source of bean definitions
 * - @EnableAutoConfiguration: Enables Spring Boot's auto-configuration mechanism
 * - @ComponentScan: Enables component scanning in this package and sub-packages
 */
@SpringBootApplication
public class FinanceTrackerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceTrackerApiApplication.class, args);
    }
}
