package com.mdl.platform.auth.config;

import com.mdl.platform.businesses.repository.BusinessRepository;
import com.mdl.platform.users.entity.User;
import com.mdl.platform.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the MDL owner account on first startup if it does not exist.
 * Passwords are hashed with BCrypt — never stored in SQL migration files.
 */
@Component
public class OwnerAccountSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerAccountSeeder.class);

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.owner.seed-enabled:true}")
    private boolean seedEnabled;

    @Value("${app.owner.email:owner@mdl.local}")
    private String ownerEmail;

    @Value("${app.owner.username:owner}")
    private String ownerUsername;

    @Value("${app.owner.password:Owner@123!}")
    private String ownerPassword;

    public OwnerAccountSeeder(
            UserRepository userRepository,
            BusinessRepository businessRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(ownerEmail)) {
            return;
        }

        var business = businessRepository.findByCode("MDL")
                .orElseThrow(() -> new IllegalStateException("MDL business not found — run Flyway migrations first"));

        User owner = new User();
        owner.setEmail(ownerEmail);
        owner.setUsername(ownerUsername);
        owner.setPasswordHash(passwordEncoder.encode(ownerPassword));
        owner.setFirstName("MDL");
        owner.setLastName("Owner");
        owner.setStatus("ACTIVE");
        owner = userRepository.save(owner);

        jdbcTemplate.update("""
                INSERT INTO user_business_memberships (user_id, business_id, is_default, status)
                VALUES (?, ?, TRUE, 'ACTIVE')
                """, owner.getId(), business.getId());

        Long ownerRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE code = 'OWNER' AND business_id IS NULL",
                Long.class);

        jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role_id, business_id)
                VALUES (?, ?, ?)
                """, owner.getId(), ownerRoleId, business.getId());

        log.info("Created MDL owner account: {} (change password after first login)", ownerEmail);
    }
}
