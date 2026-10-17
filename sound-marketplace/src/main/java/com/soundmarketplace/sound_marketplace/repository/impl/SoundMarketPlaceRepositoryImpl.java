package com.soundmarketplace.sound_marketplace.repository.impl;

import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.SoundMarketPlaceRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SoundMarketPlaceRepositoryImpl implements SoundMarketPlaceRepository {

    private final UserAccountRepository userAccountRepository;

    @Override
    public UserAccount getAllSoundMarketPlaceRepository() {
        return userAccountRepository.findAll().stream()
                .findFirst()
                .orElse(null);
    }
}
