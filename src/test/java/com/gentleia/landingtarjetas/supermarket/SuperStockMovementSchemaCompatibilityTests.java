package com.gentleia.landingtarjetas.supermarket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SuperStockMovementSchemaCompatibilityTests {

    @Test
    void migratesOldH2EnumColumnToAcceptAllStage3MovementTypes() {
        JdbcTemplate jdbcTemplate = h2JdbcTemplate();
        jdbcTemplate.execute("CREATE TABLE super_stock_movements (movement_type ENUM('ADJUSTMENT') NOT NULL)");
        insertMovementType(jdbcTemplate, "ADJUSTMENT");

        assertThatThrownBy(() -> insertMovementType(jdbcTemplate, "QUICK_CONSUMPTION"))
                .isInstanceOf(DataAccessException.class);

        SuperStockMovementSchemaCompatibility compatibility = new SuperStockMovementSchemaCompatibility(jdbcTemplate);

        assertThatCode(() -> {
            compatibility.migrateIfNeeded();
            compatibility.migrateIfNeeded();
            insertMovementType(jdbcTemplate, "PURCHASE");
            insertMovementType(jdbcTemplate, "CONSUMPTION");
            insertMovementType(jdbcTemplate, "QUICK_CONSUMPTION");
        }).doesNotThrowAnyException();
        assertThat(movementTypeColumnNullability(jdbcTemplate)).isEqualTo("NO");
    }

    @Test
    void ignoresMissingMovementTable() {
        JdbcTemplate jdbcTemplate = h2JdbcTemplate();
        SuperStockMovementSchemaCompatibility compatibility = new SuperStockMovementSchemaCompatibility(jdbcTemplate);

        assertThatCode(compatibility::migrateIfNeeded).doesNotThrowAnyException();
    }

    private static JdbcTemplate h2JdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return new JdbcTemplate(dataSource);
    }

    private static void insertMovementType(JdbcTemplate jdbcTemplate, String movementType) {
        jdbcTemplate.update("INSERT INTO super_stock_movements (movement_type) VALUES (?)", movementType);
    }

    private static String movementTypeColumnNullability(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = SCHEMA()
                  AND TABLE_NAME = 'SUPER_STOCK_MOVEMENTS'
                  AND COLUMN_NAME = 'MOVEMENT_TYPE'
                """, String.class);
    }
}
