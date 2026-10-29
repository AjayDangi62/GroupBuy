package com.groupbuy.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testcontainers-based integration test for PostgreSQL database foundation.
 * Validates Flyway migration execution, schema table creation, and core integrity constraints.
 * If Docker is unavailable in the environment, the test cleanly logs the limitation and records assumption failure.
 */
/**
 * Testcontainers-based integration test for PostgreSQL database foundation.
 * Validates Flyway migration execution, schema table creation, and core integrity constraints.
 * If Docker is unavailable in the environment, the test cleanly logs the limitation and skips via JUnit assumption.
 */
@DisplayName("PostgreSQL Database Schema & Flyway Migration Integration Test")
class PostgresSchemaMigrationTest {

    private static final Logger log = LoggerFactory.getLogger(PostgresSchemaMigrationTest.class);

    private static PostgreSQLContainer<?> postgres;
    private static boolean dockerAvailable;

    @BeforeAll
    static void initContainer() {
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception ex) {
            dockerAvailable = false;
        }

        if (dockerAvailable) {
            postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("groupbuy_test_db")
                    .withUsername("test_user")
                    .withPassword("test_password");
            postgres.start();
        } else {
            log.warn("Docker environment is NOT available on this host. Skipping PostgreSQL Testcontainers integration test.");
        }
    }

    @org.junit.jupiter.api.AfterAll
    static void tearDown() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    @Test
    @DisplayName("Verify Flyway migration creates all 14 required relational tables")
    void flywayMigrationCreatesAllTables() throws SQLException {
        Assumptions.assumeTrue(dockerAvailable, "Docker daemon is not running. Execution of Testcontainers test is blocked.");

        // Run Flyway migration against live PostgreSQL container
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        int migrationsApplied = flyway.migrate().migrationsExecuted;
        assertThat(migrationsApplied).isGreaterThanOrEqualTo(1);

        // Verify all 18 tables are present in the public schema
        List<String> expectedTables = List.of(
                "users",
                "products",
                "categories",
                "campaigns",
                "pricing_tiers",
                "participations",
                "inventory_reservations",
                "payments",
                "payment_attempts",
                "payment_events",
                "carts",
                "cart_items",
                "orders",
                "order_items",
                "refunds",
                "audit_logs",
                "idempotency_records",
                "refresh_tokens"
        );

        Set<String> actualTables = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        actualTables.add(rs.getString("table_name"));
                    }
                }
            }
        }

        for (String expectedTable : expectedTables) {
            assertThat(actualTables)
                    .as("Expected table '%s' to be created by Flyway migration", expectedTable)
                    .contains(expectedTable);
        }
    }

    @Test
    @DisplayName("Verify check constraints enforce positive product base_price")
    void checkConstraintRejectsNegativeProductPrice() throws SQLException {
        Assumptions.assumeTrue(dockerAvailable, "Docker daemon is not running. Execution of Testcontainers test is blocked.");

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            // First create a user (seller)
            long sellerId;
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO users (email, username, password_hash, full_name, role) " +
                    "VALUES ('seller@groupbuy.com', 'seller1', 'hash123', 'Sample Seller', 'SELLER') RETURNING id")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    sellerId = rs.getLong(1);
                }
            }

            // Attempt to insert product with negative base_price (-100.00)
            String insertNegativeProductSql =
                    "INSERT INTO products (seller_id, sku, name, base_price, total_inventory, available_inventory) " +
                    "VALUES (?, 'SKU-NEG', 'Negative Price Product', -100.00, 10, 10)";

            try (PreparedStatement stmt = connection.prepareStatement(insertNegativeProductSql)) {
                stmt.setLong(1, sellerId);
                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("chk_products_base_price");
            }
        }
    }

    @Test
    @DisplayName("Verify unique constraints prevent duplicate user emails")
    void uniqueConstraintPreventsDuplicateUserEmail() throws SQLException {
        Assumptions.assumeTrue(dockerAvailable, "Docker daemon is not running. Execution of Testcontainers test is blocked.");

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            try (PreparedStatement stmt1 = connection.prepareStatement(
                    "INSERT INTO users (email, username, password_hash, full_name, role) " +
                    "VALUES ('dup@groupbuy.com', 'user_dup_1', 'hash1', 'Dup User 1', 'CUSTOMER')")) {
                stmt1.executeUpdate();
            }

            try (PreparedStatement stmt2 = connection.prepareStatement(
                    "INSERT INTO users (email, username, password_hash, full_name, role) " +
                    "VALUES ('dup@groupbuy.com', 'user_dup_2', 'hash2', 'Dup User 2', 'CUSTOMER')")) {
                assertThatThrownBy(stmt2::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("uq_users_email");
            }
        }
    }
}
