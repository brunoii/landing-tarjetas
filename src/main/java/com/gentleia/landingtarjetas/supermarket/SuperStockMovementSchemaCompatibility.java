package com.gentleia.landingtarjetas.supermarket;

import java.sql.Connection;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class SuperStockMovementSchemaCompatibility implements ApplicationRunner, Ordered {

    private static final String H2_DATABASE_PRODUCT_NAME = "H2";

    private final JdbcTemplate jdbcTemplate;

    SuperStockMovementSchemaCompatibility(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateIfNeeded();
    }

    void migrateIfNeeded() {
        if (!isH2Database() || !movementTypeColumnExists()) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE super_stock_movements ALTER COLUMN movement_type SET DATA TYPE VARCHAR(40)");
    }

    private boolean isH2Database() {
        Boolean h2Database = jdbcTemplate.execute((ConnectionCallback<Boolean>) this::isH2Connection);
        return Boolean.TRUE.equals(h2Database);
    }

    private boolean isH2Connection(Connection connection) throws java.sql.SQLException {
        return H2_DATABASE_PRODUCT_NAME.equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
    }

    private boolean movementTypeColumnExists() {
        Integer columnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = SCHEMA()
                  AND TABLE_NAME = 'SUPER_STOCK_MOVEMENTS'
                  AND COLUMN_NAME = 'MOVEMENT_TYPE'
                """, Integer.class);
        return columnCount != null && columnCount > 0;
    }
}
