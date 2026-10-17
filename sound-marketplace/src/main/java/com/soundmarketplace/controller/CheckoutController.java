package com.soundmarketplace.controller;

import com.soundmarketplace.service.CheckoutService;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.CheckoutRequest;
import com.soundmarketplace.sound_marketplace.dto.CheckoutResponse;
import com.soundmarketplace.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@RequestBody CheckoutRequest request) {
        Integer buyerId = CurrentUser.getUserId();
        CheckoutResponse response = checkoutService.checkout(buyerId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/session")
    public ResponseEntity<ApiResponse<CheckoutResponse>> createSession(@RequestBody CheckoutRequest request) {
        Integer buyerId = CurrentUser.getUserId();
        CheckoutResponse response = checkoutService.createSession(buyerId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
