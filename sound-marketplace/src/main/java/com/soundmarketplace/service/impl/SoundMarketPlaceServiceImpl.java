package com.soundmarketplace.service.impl;

import com.soundmarketplace.service.SoundMarketPlaceService;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.SoundMarketPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SoundMarketPlaceServiceImpl implements SoundMarketPlaceService {

    private final SoundMarketPlaceRepository soundMarketPlaceRepository;

    @Override
    public UserAccount getFeaturedUser() {
        return soundMarketPlaceRepository.getAllSoundMarketPlaceRepository();
    }
}
