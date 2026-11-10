package com.musomi.manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Guards the production migration path from accidentally applying local demo seed records.
 */
@Testcontainers
class ProductionMigrationIsolationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test")
            .withUsername("musomi")
            .withPassword("test-only-password");

    @Test
    void productionMigrationsExcludeDemoSeedAndRemainCompatibleWithExistingDevHistory() throws Exception {
        Flyway developmentFlyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/dev-seed")
                .load();
        developmentFlyway.migrate();

        // The default local/test profile deliberately loads the seed migration.
        assertThat(count(POSTGRES.getJdbcUrl(), "SELECT COUNT(*) FROM schools")).isEqualTo(1L);
        assertThat(count(POSTGRES.getJdbcUrl(), "SELECT COUNT(*) FROM students")).isEqualTo(50L);
        assertThat(count(POSTGRES.getJdbcUrl(),
                "SELECT COUNT(*) FROM flyway_schema_history WHERE script = 'R__correct_development_seed_password_hash.sql' AND success = TRUE")).isEqualTo(1L);
        String adminHash = value(POSTGRES.getJdbcUrl(),
                "SELECT password_hash FROM users WHERE username = 'admin'");
        assertThat(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .matches("password123", adminHash)).isTrue();

        // A developer database may already have V099 recorded. The production location
        // excludes the seed file and ignores only that now-missing dev-only migration.
        Flyway productionAgainstExistingDevDatabase = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .ignoreMigrationPatterns("*:missing")
                .load();
        productionAgainstExistingDevDatabase.validate();

        // A fresh production database must contain schema only: no school or demo students.
        String databaseName = "musomi_prod_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection adminConnection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = adminConnection.createStatement()) {
            statement.execute("CREATE DATABASE " + databaseName);
        }

        String productionUrl = POSTGRES.getJdbcUrl().replace("/test?", "/" + databaseName + "?");
        Flyway productionFlyway = Flyway.configure()
                .dataSource(productionUrl, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();
        productionFlyway.migrate();

        assertThat(count(productionUrl, "SELECT COUNT(*) FROM schools")).isZero();
        assertThat(count(productionUrl, "SELECT COUNT(*) FROM students")).isZero();
        assertThat(count(productionUrl,
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '099'")).isZero();
        assertThat(count(productionUrl,
                "SELECT COUNT(*) FROM flyway_schema_history WHERE script = 'R__correct_development_seed_password_hash.sql'")).isZero();
        assertThat(count(productionUrl,
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '101'")).isEqualTo(1L);
    }

    private String value(String jdbcUrl, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private long count(String jdbcUrl, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
