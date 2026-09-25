package com.connectors.pos.ordersystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseOrderNumberGeneratorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void generatesIncreasingValuesWithSqlite() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + temporaryDirectory.resolve("orders.sqlite"));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE order_number_sequence (
                    id INTEGER PRIMARY KEY AUTOINCREMENT
                )
                """);
        jdbcTemplate.update("INSERT INTO order_number_sequence (id) VALUES (10)");

        DatabaseOrderNumberGenerator generator = new DatabaseOrderNumberGenerator(jdbcTemplate);

        assertThat(generator.nextValue()).isEqualTo(11);
        assertThat(generator.nextValue()).isEqualTo(12);
    }
}
