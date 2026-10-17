package com.soundmarketplace.controller;

import com.soundmarketplace.service.CartService;
import com.soundmarketplace.sound_marketplace.dto.AddToCartRequest;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.CartDTO;
import com.soundmarketplace.sound_marketplace.dto.UpdateCartItemRequest;
import com.soundmarketplace.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping(value = {"", "/me"})
    public ResponseEntity<ApiResponse<CartDTO>> getCart() {
        Integer currentUserId = CurrentUser.getUserId();
        CartDTO cart = cartService.getCartByUserId(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDTO>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        request.setUserId(CurrentUser.getUserId());
        CartDTO cart = cartService.addToCart(request);
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart", cart));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartDTO>> updateQuantity(
            @PathVariable Integer cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Integer currentUserId = CurrentUser.getUserId();
        CartDTO cart = cartService.updateCartItemQuantity(currentUserId, cartItemId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cart updated", cart));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartDTO>> removeItem(@PathVariable Integer cartItemId) {
        Integer currentUserId = CurrentUser.getUserId();
        CartDTO cart = cartService.removeCartItem(currentUserId, cartItemId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart", cart));
    }

    @DeleteMapping(value = {"", "/me"})
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        Integer currentUserId = CurrentUser.getUserId();
        cartService.clearCart(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }
}
