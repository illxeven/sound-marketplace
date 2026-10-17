package src.main.java.com.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@PropertySource(value = "classpath:dbconnections.properties", ignoreResourceNotFound = true)
public class SoundMarketplaceConfiguration {

    @Autowired
    private Environment environment;

    public SoundMarketplaceConfiguration() {
        super();
    }

    @Bean
    public DataSource getMysqlDataSource() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(environment.getProperty("spring.datasource.url"));
        dataSource.setUsername(environment.getProperty("spring.datasource.username"));
        dataSource.setPassword(environment.getProperty("spring.datasource.password"));
        return dataSource;
    }

    @Bean
    @Qualifier("soundMarketplaceDataSourceTemplate")
    public JdbcTemplate soundMarketplaceDataSource() throws Exception
    {
        return  new JdbcTemplate(getMysqlDataSource());
    }


}
