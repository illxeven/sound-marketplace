package com.soundmarketplace.sound_marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.soundmarketplace")
@EnableAsync
public class SoundMarketplaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoundMarketplaceApplication.class, args);
	}

}
