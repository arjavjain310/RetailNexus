package com.retailnexus.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures the products.unit column exists (for DBs created before unit was added to Product entity).
 */
@Component
@Order(1)
public class SchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE products ADD COLUMN unit VARCHAR(20) DEFAULT 'PIECES' NOT NULL"
            );
        } catch (Exception e) {
            if (!isExpectedMigrationError(e, "already exists", "Duplicate column", "Duplicate column name", "not found")) {
                throw new RuntimeException("Schema migration failed", e);
            }
        }
        try {
            jdbcTemplate.execute(
                "ALTER TABLE sale_items ALTER COLUMN quantity SET DATA TYPE DECIMAL(12,3)"
            );
        } catch (Exception e) {
            if (!isExpectedMigrationError(e, "already exists", "not found", "Duplicate", "cannot")) {
                throw new RuntimeException("Schema migration failed (sale_items.quantity)", e);
            }
        }
        dropBatchesQuantityCheckConstraint();
    }

    private static boolean isExpectedMigrationError(Throwable e, String... keywords) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String msg = t.getMessage() != null ? t.getMessage() : "";
            for (String k : keywords) {
                if (msg.contains(k)) return true;
            }
        }
        return false;
    }

    /** Drops CHECK constraint on batches.quantity so quantity can go negative on oversell. */
    private void dropBatchesQuantityCheckConstraint() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'BATCHES'",
                Integer.class
            );
            if (count == null || count == 0) return;
        } catch (Exception e) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE batches DROP CONSTRAINT CONSTRAINT_1");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("not found") || msg.contains("Unknown constraint") || msg.contains("does not exist")) return;
            try {
                List<String> names = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINTS WHERE TABLE_NAME = 'BATCHES' AND CONSTRAINT_TYPE = 'CHECK'",
                    String.class
                );
                for (String name : names) {
                    try {
                        jdbcTemplate.execute("ALTER TABLE batches DROP CONSTRAINT " + name);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
    }
}
