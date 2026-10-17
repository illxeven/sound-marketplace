package com.soundmarketplace.sound_marketplace;

import com.soundmarketplace.config.SoundMarketplaceConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SoundMarketplaceConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SoundMarketplaceConfiguration configuration;

    @Test
    @DisplayName("SoundMarketplaceConfiguration bean is loaded in context")
    void testConfigurationBeanLoaded() {
        assertThat(configuration).isNotNull();
        assertThat(applicationContext.getBean(SoundMarketplaceConfiguration.class)).isNotNull();
        assertThat(applicationContext.getBean(SoundMarketplaceConfiguration.class)).isSameAs(configuration);
    }
}
