package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.dto.AddToCartRequest;
import com.soundmarketplace.sound_marketplace.dto.CartDTO;
import com.soundmarketplace.sound_marketplace.dto.UpdateCartItemRequest;

public interface CartService {
    CartDTO getCartByUserId(Integer userId);
    CartDTO addToCart(AddToCartRequest request);
    CartDTO updateCartItemQuantity(Integer userId, Integer cartItemId, UpdateCartItemRequest request);
    CartDTO removeCartItem(Integer userId, Integer cartItemId);
    void clearCart(Integer userId);
}
