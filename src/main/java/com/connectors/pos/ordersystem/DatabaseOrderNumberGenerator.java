package com.connectors.pos.ordersystem;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

@Component
public class DatabaseOrderNumberGenerator implements OrderNumberGenerator {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseOrderNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long nextValue() {
        Long value = jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            String database = connection.getMetaData().getDatabaseProductName()
                    .toLowerCase(Locale.ROOT);

            if (database.contains("sqlite")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO order_number_sequence DEFAULT VALUES");
                    try (ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
                        if (!result.next()) {
                            throw new IllegalStateException("SQLite did not return an order sequence value");
                        }
                        return result.getLong(1);
                    }
                }
            }

            if (database.contains("postgresql")) {
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT nextval('order_number_seq')")) {
                    if (!result.next()) {
                        throw new IllegalStateException("PostgreSQL did not return an order sequence value");
                    }
                    return result.getLong(1);
                }
            }

            throw new IllegalStateException("Unsupported database for order-number generation: " + database);
        });

        if (value == null) {
            throw new IllegalStateException("Database did not return an order sequence value");
        }
        return value;
    }
}
