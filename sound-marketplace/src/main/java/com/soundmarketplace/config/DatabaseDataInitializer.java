package com.soundmarketplace.config;

import com.soundmarketplace.sound_marketplace.repository.MerchandiseTypeRepository;
import com.soundmarketplace.sound_marketplace.domain.MerchandiseType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseDataInitializer implements CommandLineRunner {

    private final MerchandiseTypeRepository merchandiseTypeRepository;

    @Override
    public void run(String... args) throws Exception {
        // Seed merchandise types if empty
        if (merchandiseTypeRepository.count() == 0) {
            List<String> types = Arrays.asList(
                "Sample Pack",
                "Clothing & Apparel",
                "Merch & Gear",
                "Beat License",
                "Preset Bank",
                "Sound Kit"
            );
            for (String t : types) {
                MerchandiseType type = new MerchandiseType();
                type.setTypeName(t);
                merchandiseTypeRepository.save(type);
            }
            merchandiseTypeRepository.flush();
            log.info("Initialized merchandise types.");
        }
    }
}
