package com.soundmarketplace.sound_marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.soundmarketplace")
public class SoundMarketplaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoundMarketplaceApplication.class, args);
	}

}
