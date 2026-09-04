package com.mdl.platform.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Repairs Flyway checksum drift in local development when migration files were
 * edited after being applied (common during active development).
 */
@Configuration
@Profile("dev")
public class DevFlywayConfig {

    @Bean
    public FlywayMigrationStrategy devFlywayMigrationStrategy() {
        return (Flyway flyway) -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
