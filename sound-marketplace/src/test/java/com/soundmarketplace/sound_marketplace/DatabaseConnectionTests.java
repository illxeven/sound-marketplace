package com.soundmarketplace.sound_marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabaseConnectionTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testConnection() {
        assertThat(jdbcTemplate).isNotNull();
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
        System.out.println("Database connection verified successfully using JdbcTemplate!");
    }
}
