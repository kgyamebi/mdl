package com.mdl.platform.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ProductionStartupValidatorTest {

    private ProductionStartupValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProductionStartupValidator();
        ReflectionTestUtils.setField(validator, "dbPassword", "strong-db-password-for-production");
    }

    @Test
    void acceptsStrongProductionSecret() {
        ReflectionTestUtils.setField(validator, "jwtSecret",
                "prod-secret-with-enough-entropy-for-hs256-signing-key");
        ReflectionTestUtils.setField(validator, "ownerSeedEnabled", false);
        ReflectionTestUtils.setField(validator, "demoSeedEnabled", false);
        ReflectionTestUtils.setField(validator, "ownerPassword", "");

        assertThatCode(validator::validateProductionConfiguration).doesNotThrowAnyException();
    }

    @Test
    void rejectsDevelopmentJwtSecret() {
        ReflectionTestUtils.setField(validator, "jwtSecret",
                "mdl-dev-secret-change-in-production-must-be-32-chars-minimum");
        ReflectionTestUtils.setField(validator, "ownerSeedEnabled", false);
        ReflectionTestUtils.setField(validator, "demoSeedEnabled", false);

        assertThatThrownBy(validator::validateProductionConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("development default");
    }

    @Test
    void rejectsPlaceholderJwtSecret() {
        ReflectionTestUtils.setField(validator, "jwtSecret", "change_me_in_production_must_be_long_enough");
        ReflectionTestUtils.setField(validator, "ownerSeedEnabled", false);
        ReflectionTestUtils.setField(validator, "demoSeedEnabled", false);

        assertThatThrownBy(validator::validateProductionConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void rejectsShortJwtSecret() {
        ReflectionTestUtils.setField(validator, "jwtSecret", "too-short");
        ReflectionTestUtils.setField(validator, "ownerSeedEnabled", false);
        ReflectionTestUtils.setField(validator, "demoSeedEnabled", false);

        assertThatThrownBy(validator::validateProductionConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    void rejectsWeakDatabasePassword() {
        ReflectionTestUtils.setField(validator, "jwtSecret",
                "prod-secret-with-enough-entropy-for-hs256-signing-key");
        ReflectionTestUtils.setField(validator, "dbPassword", "password");
        ReflectionTestUtils.setField(validator, "ownerSeedEnabled", false);
        ReflectionTestUtils.setField(validator, "demoSeedEnabled", false);

        assertThatThrownBy(validator::validateProductionConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    void rejectsDemoOwnerPassword() {
        ReflectionTestUtils.setField(validator, "jwtSecret",
                "prod-secret-with-enough-entropy-for-hs256-signing-key");
        ReflectionTestUtils.setField(validator, "ownerSeedEnabled", true);
        ReflectionTestUtils.setField(validator, "ownerPassword", "Owner@123!");
        ReflectionTestUtils.setField(validator, "demoSeedEnabled", false);

        assertThatThrownBy(validator::validateProductionConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demo default");
    }

    @Test
    void allowsOwnerSeedForBootstrapInProduction() {
        ReflectionTestUtils.setField(validator, "jwtSecret",
                "prod-secret-with-enough-entropy-for-hs256-signing-key");
        ReflectionTestUtils.setField(validator, "ownerSeedEnabled", true);
        ReflectionTestUtils.setField(validator, "ownerPassword", "SecureOwnerPass123!");
        ReflectionTestUtils.setField(validator, "demoSeedEnabled", false);

        assertThatCode(validator::validateProductionConfiguration).doesNotThrowAnyException();
    }

    @Test
    void rejectsDemoSeedInProduction() {
        ReflectionTestUtils.setField(validator, "jwtSecret",
                "prod-secret-with-enough-entropy-for-hs256-signing-key");
        ReflectionTestUtils.setField(validator, "ownerSeedEnabled", false);
        ReflectionTestUtils.setField(validator, "demoSeedEnabled", true);

        assertThatThrownBy(validator::validateProductionConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_SEED_ENABLED");
    }
}
