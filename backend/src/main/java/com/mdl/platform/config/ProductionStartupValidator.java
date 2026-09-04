package com.mdl.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Fail fast in production when critical security settings are misconfigured.
 */
@Component
@Profile("prod")
public class ProductionStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionStartupValidator.class);
    private static final String DEV_JWT_SECRET =
            "mdl-dev-secret-change-in-production-must-be-32-chars-minimum";
    private static final Set<String> WEAK_DB_PASSWORDS = Set.of(
            "mdl_password",
            "mdl_user",
            "password",
            "changeme",
            "change_me",
            "root",
            "admin",
            "12345678");

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.owner.seed-enabled:false}")
    private boolean ownerSeedEnabled;

    @Value("${app.owner.password:}")
    private String ownerPassword;

    @Value("${app.demo.seed-enabled:false}")
    private boolean demoSeedEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionConfiguration() {
        validateJwtSecret();
        validateDatabasePassword();
        validateOwnerPassword();
        warnIfOwnerSeedingEnabled();
        rejectDemoSeeding();
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

    private void validateDatabasePassword() {
        if (dbPassword == null || dbPassword.isBlank()) {
            throw new IllegalStateException("DB_PASSWORD must be set in production");
        }
        if (dbPassword.length() < 16) {
            throw new IllegalStateException("DB_PASSWORD must be at least 16 characters in production");
        }
        String normalized = dbPassword.toLowerCase();
        if (WEAK_DB_PASSWORDS.contains(normalized)) {
            throw new IllegalStateException("DB_PASSWORD is too weak for production");
        }
        if (normalized.contains("change_me") || normalized.contains("example")) {
            throw new IllegalStateException("DB_PASSWORD appears to be a placeholder — set a strong production password");
        }
    }

    private void validateOwnerPassword() {
        if (ownerSeedEnabled && (ownerPassword == null || ownerPassword.isBlank())) {
            throw new IllegalStateException("OWNER_PASSWORD must be set when OWNER_SEED_ENABLED is true");
        }
        if ("Owner@123!".equals(ownerPassword)) {
            throw new IllegalStateException("OWNER_PASSWORD must not use the demo default in production");
        }
        if (ownerPassword != null && !ownerPassword.isBlank() && ownerPassword.length() < 12) {
            throw new IllegalStateException("OWNER_PASSWORD must be at least 12 characters in production");
        }
    }

    private void warnIfOwnerSeedingEnabled() {
        if (ownerSeedEnabled) {
            log.warn(
                    "OWNER_SEED_ENABLED is true in production — owner is created only if missing; "
                            + "set OWNER_SEED_ENABLED=false after first login");
        }
    }

    private void rejectDemoSeeding() {
        if (demoSeedEnabled) {
            throw new IllegalStateException("DEMO_SEED_ENABLED must be false in production");
        }
    }
}
