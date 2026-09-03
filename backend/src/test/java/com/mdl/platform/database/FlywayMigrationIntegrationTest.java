package com.mdl.platform.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies all Flyway migrations apply cleanly on MariaDB and seed data is correct.
 * Requires Docker. Skipped automatically when Docker is not available.
 */
@SpringBootTest
@Testcontainers
@EnabledIf("com.mdl.platform.support.DockerTestSupport#isDockerAvailable")
class FlywayMigrationIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesAllMigrations() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT MAX(version) FROM flyway_schema_history", Integer.class);
        assertThat(version).isEqualTo(26);
    }

    @Test
    void mdlBusinessIsSeeded() {
        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM businesses WHERE code = 'MDL'", String.class);
        assertThat(name).isEqualTo("Modern Dream Light");
    }

    @Test
    void mdlUsesGhsCurrency() {
        String currency = jdbcTemplate.queryForObject(
                "SELECT currency_code FROM businesses WHERE code = 'MDL'", String.class);
        assertThat(currency).isEqualTo("GHS");
    }

    @Test
    void mdlHasTwoMainWarehouses() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM warehouses w
                JOIN businesses b ON b.id = w.business_id
                WHERE b.code = 'MDL' AND w.warehouse_type = 'MAIN' AND w.is_restricted = TRUE
                """, Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void mdlHasThreeShopsWithWarehouses() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM shops s
                JOIN businesses b ON b.id = s.business_id
                WHERE b.code = 'MDL' AND s.warehouse_id IS NOT NULL
                """, Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void ownerRoleHasAllPermissions() {
        Integer permissionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM permissions", Integer.class);
        Integer ownerPermissionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id
                WHERE r.code = 'OWNER'
                """, Integer.class);
        assertThat(ownerPermissionCount).isEqualTo(permissionCount);
    }

    @Test
    void transferRoutesExistFromMainToShops() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM warehouse_transfer_routes r
                JOIN warehouses fw ON fw.id = r.from_warehouse_id
                JOIN warehouses tw ON tw.id = r.to_warehouse_id
                JOIN businesses b ON b.id = r.business_id
                WHERE b.code = 'MDL'
                  AND fw.warehouse_type = 'MAIN'
                  AND tw.warehouse_type = 'SHOP'
                  AND r.enabled = TRUE
                """, Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(6);
    }

    @Test
    void mdlHasSeededProducts() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM products p
                JOIN businesses b ON b.id = p.business_id
                WHERE b.code = 'MDL' AND p.status = 'ACTIVE'
                """, Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(16);
    }

    @Test
    void mdlProductsHaveBarcodes() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM barcodes bc
                JOIN businesses b ON b.id = bc.business_id
                WHERE b.code = 'MDL'
                """, Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(13);
    }

    @Test
    void mdlHasSeededInventoryBalances() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_balances ib
                JOIN businesses b ON b.id = ib.business_id
                WHERE b.code = 'MDL'
                """, Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(12);
    }

    @Test
    void inventoryTransactionsMatchBalances() {
        Integer txnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_transactions t
                JOIN businesses b ON b.id = t.business_id
                WHERE b.code = 'MDL' AND t.transaction_type = 'OPENING_BALANCE'
                """, Integer.class);
        Integer balanceCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_balances ib
                JOIN businesses b ON b.id = ib.business_id
                WHERE b.code = 'MDL'
                """, Integer.class);
        assertThat(txnCount).isEqualTo(balanceCount);
    }
}
