package com.soundmarketplace.controller;

import com.soundmarketplace.service.CartService;
import com.soundmarketplace.sound_marketplace.dto.AddToCartRequest;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.CartDTO;
import com.soundmarketplace.sound_marketplace.dto.UpdateCartItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<CartDTO>> getCart(@PathVariable Integer userId) {
        CartDTO cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDTO>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        CartDTO cart = cartService.addToCart(request);
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart", cart));
    }

    @PutMapping("/user/{userId}/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartDTO>> updateQuantity(
            @PathVariable Integer userId,
            @PathVariable Integer cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartDTO cart = cartService.updateCartItemQuantity(userId, cartItemId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cart updated", cart));
    }

    @DeleteMapping("/user/{userId}/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartDTO>> removeItem(
            @PathVariable Integer userId,
            @PathVariable Integer cartItemId) {
        CartDTO cart = cartService.removeCartItem(userId, cartItemId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart", cart));
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable Integer userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }
}
