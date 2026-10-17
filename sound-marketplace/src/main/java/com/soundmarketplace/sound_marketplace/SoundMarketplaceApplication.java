package com.soundmarketplace.sound_marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import src.main.java.com.config.SecurityConfig;
import src.main.java.com.config.SoundMarketplaceConfiguration;

@SpringBootApplication(scanBasePackages = {"com.soundmarketplace", "src.main.java.com.config"})
@Import({SoundMarketplaceConfiguration.class, SecurityConfig.class})
public class SoundMarketplaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoundMarketplaceApplication.class, args);
	}

}
