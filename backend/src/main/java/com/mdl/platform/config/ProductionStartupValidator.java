package com.mdl.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Fail fast in production when critical security settings are misconfigured.
 */
@Component
@Profile("prod")
public class ProductionStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionStartupValidator.class);
    private static final String DEV_JWT_SECRET =
            "mdl-dev-secret-change-in-production-must-be-32-chars-minimum";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.owner.seed-enabled:false}")
    private boolean ownerSeedEnabled;

    @Value("${app.demo.seed-enabled:false}")
    private boolean demoSeedEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionConfiguration() {
        validateJwtSecret();
        warnIfDemoSeedingEnabled();
    }

    private void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be set in production");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters in production");
        }
        if (DEV_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET must not use the development default in production");
        }
        String normalized = jwtSecret.toLowerCase();
        if (normalized.contains("change_me") || normalized.contains("change-this")) {
            throw new IllegalStateException("JWT_SECRET appears to be a placeholder — set a strong production secret");
        }
    }

    private void warnIfDemoSeedingEnabled() {
        if (ownerSeedEnabled) {
            log.warn(
                    "OWNER_SEED_ENABLED is true in production — owner is created only if missing; "
                            + "set OWNER_SEED_ENABLED=false after first login");
        }
        if (demoSeedEnabled) {
            throw new IllegalStateException("DEMO_SEED_ENABLED must be false in production");
        }
    }
}
