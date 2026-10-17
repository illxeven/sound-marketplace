package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.dto.CheckoutRequest;
import com.soundmarketplace.sound_marketplace.dto.CheckoutResponse;

public interface CheckoutService {
    CheckoutResponse checkout(Integer buyerId, CheckoutRequest request);
    CheckoutResponse createSession(Integer buyerId, CheckoutRequest request);
}
