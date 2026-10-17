package com.soundmarketplace.controller;

import com.soundmarketplace.service.MarketplaceService;
import com.soundmarketplace.service.OrderService;
import com.soundmarketplace.service.PaymentService;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.PaymentHistoryDTO;
import com.soundmarketplace.sound_marketplace.dto.SavedPaymentMethodDTO;
import com.soundmarketplace.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final MarketplaceService marketplaceService;
    private final PaymentService paymentService;
    private final OrderService orderService;

    @GetMapping({"/history", "/me/history", "/buyer/{buyerId}"})
    public ResponseEntity<ApiResponse<List<PaymentHistoryDTO>>> getPaymentHistory(
            @PathVariable(required = false) Integer buyerId) {
        if (buyerId != null) {
            CurrentUser.requireOwnerOrAdmin(buyerId);
        } else {
            buyerId = CurrentUser.getUserId();
        }
        List<PaymentHistoryDTO> history = marketplaceService.getPaymentHistoryByBuyer(buyerId);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/saved-cards")
    public ResponseEntity<ApiResponse<List<SavedPaymentMethodDTO>>> getSavedCards() {
        Integer buyerId = CurrentUser.getUserId();
        List<SavedPaymentMethodDTO> cards = paymentService.getSavedPaymentMethods(buyerId);
        return ResponseEntity.ok(ApiResponse.ok("Saved cards retrieved", cards));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        orderService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("Webhook processed");
    }
}
