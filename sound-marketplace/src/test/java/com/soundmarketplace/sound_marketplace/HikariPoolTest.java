package com.soundmarketplace.sound_marketplace;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HikariPoolTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Verify that auto-configured DataSource is HikariDataSource with pool size 10")
    void testDataSourceIsHikariPool() throws Exception {
        assertThat(dataSource).isNotNull();
        HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);
        assertThat(hikariDataSource).isNotNull();
        assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(10);
        assertThat(hikariDataSource.getConnectionTimeout()).isEqualTo(3000);
    }
}
