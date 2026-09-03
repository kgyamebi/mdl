package com.mdl.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the MDL (Modern Dream Light) business management platform.
 * Modular monolith — all domain modules live under com.mdl.platform.*
 */
@SpringBootApplication
public class MdlPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MdlPlatformApplication.class, args);
    }
}
