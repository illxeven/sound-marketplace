package com.soundmarketplace.sound_marketplace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import src.main.java.com.config.SoundMarketplaceConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SoundMarketplaceConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SoundMarketplaceConfiguration configuration;

    @Autowired
    @Qualifier("getMysqlDataSource")
    private DataSource mysqlDataSource;

    @Autowired
    @Qualifier("soundMarketplaceDataSourceTemplate")
    private JdbcTemplate customJdbcTemplate;

    @Nested
    @DisplayName("Spring Boot Integration Tests for SoundMarketplaceConfiguration")
    class SpringBootIntegrationTests {

        @Test
        @DisplayName("SoundMarketplaceConfiguration bean is loaded in context")
        void testConfigurationBeanLoaded() {
            assertThat(configuration).isNotNull();
            assertThat(applicationContext.getBean(SoundMarketplaceConfiguration.class)).isNotNull();
            assertThat(applicationContext.getBean(SoundMarketplaceConfiguration.class)).isSameAs(configuration);
        }

        @Test
        @DisplayName("getMysqlDataSource bean is created as DriverManagerDataSource with configured MySQL properties")
        void testMysqlDataSourceBean() {
            assertThat(mysqlDataSource).isNotNull();
            assertThat(mysqlDataSource).isInstanceOf(DriverManagerDataSource.class);

            DriverManagerDataSource dmDataSource = (DriverManagerDataSource) mysqlDataSource;
            assertThat(dmDataSource.getUrl()).contains("jdbc:mysql://localhost:3306/sound_marketplace_db");
            assertThat(dmDataSource.getUsername()).isEqualTo("sound_app_user");
        }

        @Test
        @DisplayName("soundMarketplaceDataSource JdbcTemplate bean is created and qualified")
        void testSoundMarketplaceDataSourceTemplateBean() {
            assertThat(customJdbcTemplate).isNotNull();
            assertThat(customJdbcTemplate.getDataSource()).isNotNull();
            assertThat(customJdbcTemplate.getDataSource()).isSameAs(mysqlDataSource);

            // Verify bean lookup by name from application context
            JdbcTemplate beanByName = applicationContext.getBean("soundMarketplaceDataSource", JdbcTemplate.class);
            assertThat(beanByName).isNotNull();
            assertThat(beanByName).isSameAs(customJdbcTemplate);
        }

        @Test
        @DisplayName("soundMarketplaceDataSource JdbcTemplate can query the database")
        void testJdbcTemplateDatabaseQuery() {
            Integer result = customJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("getMysqlDataSource can establish a valid database connection")
        void testDataSourceCanGetConnection() throws Exception {
            try (Connection connection = mysqlDataSource.getConnection()) {
                assertThat(connection).isNotNull();
                assertThat(connection.isValid(5)).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Isolated Unit Tests for SoundMarketplaceConfiguration")
    class IsolatedUnitTests {

        @Test
        @DisplayName("getMysqlDataSource configures DriverManagerDataSource using Environment properties")
        void testGetMysqlDataSourceUnit() throws Exception {
            SoundMarketplaceConfiguration config = new SoundMarketplaceConfiguration();
            MockEnvironment mockEnv = new MockEnvironment();
            mockEnv.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/test_unit_db");
            mockEnv.setProperty("spring.datasource.username", "unit_test_user");
            mockEnv.setProperty("spring.datasource.password", "unit_test_password");

            ReflectionTestUtils.setField(config, "environment", mockEnv);

            DataSource ds = config.getMysqlDataSource();
            assertThat(ds).isNotNull();
            assertThat(ds).isInstanceOf(DriverManagerDataSource.class);

            DriverManagerDataSource dmds = (DriverManagerDataSource) ds;
            assertThat(dmds.getUrl()).isEqualTo("jdbc:mysql://localhost:3306/test_unit_db");
            assertThat(dmds.getUsername()).isEqualTo("unit_test_user");
            assertThat(dmds.getPassword()).isEqualTo("unit_test_password");
        }

        @Test
        @DisplayName("soundMarketplaceDataSource creates a JdbcTemplate wrapping the configured DataSource")
        void testSoundMarketplaceDataSourceUnit() throws Exception {
            SoundMarketplaceConfiguration config = new SoundMarketplaceConfiguration();
            MockEnvironment mockEnv = new MockEnvironment();
            mockEnv.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/test_unit_db");
            mockEnv.setProperty("spring.datasource.username", "unit_test_user");
            mockEnv.setProperty("spring.datasource.password", "unit_test_password");

            ReflectionTestUtils.setField(config, "environment", mockEnv);

            JdbcTemplate template = config.soundMarketplaceDataSource();
            assertThat(template).isNotNull();
            assertThat(template.getDataSource()).isNotNull();
            assertThat(template.getDataSource()).isInstanceOf(DriverManagerDataSource.class);

            DriverManagerDataSource dmds = (DriverManagerDataSource) template.getDataSource();
            assertThat(dmds.getUrl()).isEqualTo("jdbc:mysql://localhost:3306/test_unit_db");
            assertThat(dmds.getUsername()).isEqualTo("unit_test_user");
        }
    }
}
