package com.mdl.platform.users.config;

import com.mdl.platform.businesses.repository.BusinessRepository;
import com.mdl.platform.users.entity.User;
import com.mdl.platform.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds demo employees for MDL so you can test role-based access before real staff are added.
 */
@Component
@Order(2)
public class DemoEmployeeSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoEmployeeSeeder.class);

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.seed-enabled:true}")
    private boolean seedEnabled;

    public DemoEmployeeSeeder(
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

        var business = businessRepository.findByCode("MDL").orElse(null);
        if (business == null) {
            return;
        }

        seedEmployee(
                business.getId(),
                "john@mdl.local",
                "john",
                "John",
                "Mensah",
                "Worker@123!",
                "SHOP_WORKER",
                new String[]{"LOC-SHOP-A", "LOC-WH-A"});

        seedEmployee(
                business.getId(),
                "michael@mdl.local",
                "michael",
                "Michael",
                "Owusu",
                "Manager@123!",
                "SHOP_MANAGER",
                new String[]{"LOC-SHOP-A", "LOC-WH-A"});

        seedEmployee(
                business.getId(),
                "receiver@mdl.local",
                "receiver",
                "Ama",
                "Boateng",
                "Receiver@123!",
                "IMPORT_RECEIVING_STAFF",
                new String[]{});
    }

    private void seedEmployee(
            Long businessId,
            String email,
            String username,
            String firstName,
            String lastName,
            String password,
            String roleCode,
            String[] locationCodes) {

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus("ACTIVE");
        user = userRepository.save(user);

        jdbcTemplate.update("""
                INSERT INTO user_business_memberships (user_id, business_id, is_default, status)
                VALUES (?, ?, TRUE, 'ACTIVE')
                """, user.getId(), businessId);

        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE code = ? AND business_id IS NULL",
                Long.class,
                roleCode);

        jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role_id, business_id)
                VALUES (?, ?, ?)
                """, user.getId(), roleId, businessId);

        for (String locationCode : locationCodes) {
            Long locationId = jdbcTemplate.queryForObject(
                    "SELECT id FROM locations WHERE business_id = ? AND code = ?",
                    Long.class,
                    businessId,
                    locationCode);

            jdbcTemplate.update("""
                    INSERT INTO user_location_assignments (user_id, business_id, location_id, access_level)
                    VALUES (?, ?, ?, 'FULL')
                    """, user.getId(), businessId, locationId);
        }

        log.info("Created demo employee: {} ({})", email, roleCode);
    }
}
