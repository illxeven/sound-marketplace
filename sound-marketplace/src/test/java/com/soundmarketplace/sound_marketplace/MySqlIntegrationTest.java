package com.soundmarketplace.sound_marketplace;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(DisabledIfDockerUnavailableCondition.class)
@SpringBootTest
@Testcontainers
@ActiveProfiles("it")
public abstract class MySqlIntegrationTest {

    @Container
    @ServiceConnection
    protected static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("sound_marketplace_db")
            .withUsername("test_user")
            .withPassword("test_password");
}
