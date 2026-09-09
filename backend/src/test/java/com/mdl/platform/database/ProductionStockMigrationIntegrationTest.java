package com.mdl.platform.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("prod")
@EnabledIf("com.mdl.platform.support.DockerTestSupport#isDockerAvailable")
class ProductionStockMigrationIntegrationTest {

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("mdl_production_stock_test")
            .withUsername("test")
            .withPassword("strong-test-db-password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("app.jwt.secret",
                () -> "production-stock-test-secret-with-more-than-32-characters");
        registry.add("app.owner.seed-enabled", () -> false);
        registry.add("app.owner.password", () -> "StrongOwnerTestPassword!");
        registry.add("app.demo.seed-enabled", () -> false);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void productionImportsOnlyCleanedActualStock() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1",
                Integer.class);
        assertThat(version).isEqualTo(40);

        Integer products = count("SELECT COUNT(*) FROM products");
        Integer balances = count("SELECT COUNT(*) FROM inventory_balances");
        Integer categories = count("SELECT COUNT(*) FROM product_categories");
        Integer negativeBalances = count(
                "SELECT COUNT(*) FROM inventory_balances WHERE quantity_on_hand < 0");

        assertThat(products).isEqualTo(3250);
        assertThat(balances).isEqualTo(3250);
        // Eight imported categories plus "Security & Safety", added in V40.
        assertThat(categories).isEqualTo(9);
        assertThat(negativeBalances).isEqualTo(27);
    }

    @Test
    void catchAllCategoryIsReducedToRealTools() {
        Integer stillInCatchAll = count("""
                SELECT COUNT(*) FROM products p
                JOIN product_categories c ON c.id = p.category_id
                WHERE c.code = 'TOOLS_AND_ACCESSORIES'
                """);
        // The import script used this category as its fallback, leaving 312 products
        // in it. V40 reclassifies everything that is really a socket, fitting or
        // breaker, leaving only genuine consumables (tape, screws, glue).
        assertThat(stillInCatchAll).isLessThan(40);

        Integer misfiled = count("""
                SELECT COUNT(*) FROM products p
                JOIN product_categories c ON c.id = p.category_id
                WHERE c.code = 'TOOLS_AND_ACCESSORIES'
                  AND (p.name LIKE '%SOCK%' OR p.name LIKE '%C/O%' OR p.name LIKE '%W/P%')
                """);
        assertThat(misfiled).isZero();
    }

    @Test
    void reorderLevelsAreNoLongerAllZero() {
        Integer placeholderLevels = count("SELECT COUNT(*) FROM products WHERE reorder_level IS NULL");
        assertThat(placeholderLevels).isZero();

        // Items the business actually holds should now carry a forward-looking
        // trigger rather than only firing once they hit zero.
        Integer withTrigger = count("SELECT COUNT(*) FROM products WHERE reorder_level > 0");
        assertThat(withTrigger).isGreaterThan(1000);

        // A reorder level must never sit at or above current stock, or the item
        // would be reported as low the moment it is counted.
        Integer flaggedWhileInStock = count("""
                SELECT COUNT(*) FROM inventory_balances b
                JOIN products p ON p.id = b.product_id
                WHERE b.quantity_on_hand > 0 AND b.quantity_on_hand <= p.reorder_level
                """);
        assertThat(flaggedWhileInStock).isZero();
    }

    @Test
    void productionHasOnlyTheRealStockLocations() {
        Integer activeLocations = count(
                "SELECT COUNT(*) FROM locations WHERE status = 'ACTIVE'");
        assertThat(activeLocations).isEqualTo(7);

        Integer expectedNames = count("""
                SELECT COUNT(*) FROM locations
                WHERE status = 'ACTIVE'
                  AND name IN (
                    'MODERN DREAM A SHOP',
                    'MODERN DREAM A WAREHOUSE A',
                    'MODERN DREAM A WAREHOUSE B',
                    'MODERN DREAM B SHOP',
                    'MODERN DREAM B WAREHOUSE C',
                    'MODERN DREAM B WAREHOUSE D',
                    'MODERN DREAM MAIN WAREHOUSE'
                  )
                """);
        assertThat(expectedNames).isEqualTo(7);
    }

    @Test
    void demoShopCStructureIsRemoved() {
        assertThat(count("SELECT COUNT(*) FROM locations WHERE code IN ('LOC-SHOP-C', 'LOC-WH-C')"))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM warehouses WHERE code = 'WH-SHOP-C'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM shops WHERE code = 'SHOP-C'")).isZero();
    }

    @Test
    void aMainWarehouseExistsSoImportsHaveADestination() {
        Integer mainWarehouses = count(
                "SELECT COUNT(*) FROM warehouses WHERE warehouse_type = 'MAIN' AND status = 'ACTIVE'");
        assertThat(mainWarehouses).isEqualTo(1);

        Integer shopWarehouses = count(
                "SELECT COUNT(*) FROM warehouses WHERE warehouse_type = 'SHOP' AND status = 'ACTIVE'");
        Integer routesFromMain = count("""
                SELECT COUNT(*) FROM warehouse_transfer_routes r
                JOIN warehouses mw ON mw.id = r.from_warehouse_id
                WHERE mw.warehouse_type = 'MAIN'
                """);
        assertThat(routesFromMain).isEqualTo(shopWarehouses);
    }

    @Test
    void mergedStockIsAssignedOnlyToShopLocations() {
        Integer shopABalances = count("""
                SELECT COUNT(*) FROM inventory_balances ib
                JOIN locations l ON l.id = ib.location_id
                WHERE l.code = 'LOC-SHOP-A'
                """);
        Integer shopBBalances = count("""
                SELECT COUNT(*) FROM inventory_balances ib
                JOIN locations l ON l.id = ib.location_id
                WHERE l.code = 'LOC-SHOP-B'
                """);
        Integer warehouseBalances = count("""
                SELECT COUNT(*) FROM inventory_balances ib
                JOIN locations l ON l.id = ib.location_id
                WHERE l.location_type = 'WAREHOUSE'
                """);

        assertThat(shopABalances).isEqualTo(2540);
        assertThat(shopBBalances).isEqualTo(710);
        assertThat(warehouseBalances).isZero();
    }

    private Integer count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
