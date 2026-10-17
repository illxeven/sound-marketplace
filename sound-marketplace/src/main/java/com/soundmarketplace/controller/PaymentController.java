package com.soundmarketplace.controller;

import com.soundmarketplace.util.CurrentUser;
import com.soundmarketplace.service.MarketplaceService;
import com.soundmarketplace.service.PaymentService;
import com.soundmarketplace.service.StripeService;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.CreatePaymentIntentRequest;
import com.soundmarketplace.sound_marketplace.dto.CreatePaymentRequest;
import com.soundmarketplace.sound_marketplace.dto.PaymentHistoryDTO;
import com.soundmarketplace.sound_marketplace.dto.PaymentIntentResponse;
import com.soundmarketplace.sound_marketplace.dto.SavedPaymentMethodDTO;
import com.soundmarketplace.sound_marketplace.dto.StripeSessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final MarketplaceService marketplaceService;
    private final StripeService stripeService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentHistoryDTO>> processPayment(@RequestBody CreatePaymentRequest request) {
        if (request != null) {
            request.setBuyerId(CurrentUser.getUserId());
        }
        PaymentHistoryDTO payment = marketplaceService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Payment processed successfully", payment));
    }

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

    @PostMapping("/buy/track/{trackId}")
    public ResponseEntity<ApiResponse<PaymentHistoryDTO>> buyTrack(
            @PathVariable Integer trackId,
            @RequestParam(required = false) String stripeChargeId) {
        Integer buyerId = CurrentUser.getUserId();
        PaymentHistoryDTO payment = marketplaceService.buyTrack(buyerId, trackId, stripeChargeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Track purchased successfully", payment));
    }

    @PostMapping("/buy/merchandise/{merchandiseId}")
    public ResponseEntity<ApiResponse<PaymentHistoryDTO>> buyMerchandise(
            @PathVariable Integer merchandiseId,
            @RequestParam(required = false) String stripeChargeId) {
        Integer buyerId = CurrentUser.getUserId();
        PaymentHistoryDTO payment = marketplaceService.buyMerchandiseItem(buyerId, merchandiseId, stripeChargeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Merchandise purchased successfully", payment));
    }

    @PostMapping("/buy/album/{albumId}")
    public ResponseEntity<ApiResponse<PaymentHistoryDTO>> buyAlbum(
            @PathVariable Integer albumId,
            @RequestParam(required = false) String stripeChargeId) {
        Integer buyerId = CurrentUser.getUserId();
        PaymentHistoryDTO payment = marketplaceService.buyAlbum(buyerId, albumId, stripeChargeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Album purchased successfully", payment));
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<ApiResponse<StripeSessionResponse>> createCheckoutSession(
            @RequestParam BigDecimal amount,
            @RequestParam(required = false, defaultValue = "USD") String currency,
            @RequestParam String itemName,
            @RequestParam(required = false, defaultValue = "https://soundmarketplace.com/success") String successUrl,
            @RequestParam(required = false, defaultValue = "https://soundmarketplace.com/cancel") String cancelUrl,
            @RequestBody(required = false) Map<String, String> metadata) {
        StripeSessionResponse session = stripeService.createCheckoutSession(amount, currency, itemName, successUrl, cancelUrl, metadata);
        return ResponseEntity.ok(ApiResponse.ok("Stripe checkout session initialized", session));
    }

    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            @RequestBody(required = false) CreatePaymentIntentRequest request,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String itemDetails,
            @RequestParam(required = false) String description) {

        CreatePaymentIntentRequest intentRequest = request;
        if (intentRequest == null) {
            intentRequest = CreatePaymentIntentRequest.builder()
                    .amount(amount)
                    .currency(currency)
                    .itemDetails(itemDetails != null ? itemDetails : description)
                    .buyerId(CurrentUser.getUserId())
                    .build();
        } else {
            intentRequest.setBuyerId(CurrentUser.getUserId());
            if (intentRequest.getAmount() == null && amount != null) {
                intentRequest.setAmount(amount);
            }
            if (intentRequest.getCurrency() == null && currency != null) {
                intentRequest.setCurrency(currency);
            }
            if (intentRequest.getItemDetails() == null && (itemDetails != null || description != null)) {
                intentRequest.setItemDetails(itemDetails != null ? itemDetails : description);
            }
        }

        log.info("--> [PaymentController] POST /api/payments/create-intent: amount={}, currency={}, itemDetails={}, buyerId={}",
                intentRequest.getAmount(), intentRequest.getCurrency(), intentRequest.getItemDetails(), intentRequest.getBuyerId());

        PaymentIntentResponse intent = paymentService.createPaymentIntent(intentRequest);
        log.info("<-- [PaymentController] Generated PaymentIntent ID: {}, Status: {}", intent.getPaymentIntentId(), intent.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Stripe payment intent initialized", intent));
    }

    @PostMapping("/confirm-intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> confirmPaymentIntent(
            @RequestParam(required = false) String paymentIntentId,
            @RequestParam(required = false) String paymentMethodId,
            @RequestBody(required = false) Map<String, String> body) {

        String id = paymentIntentId;
        String pm = paymentMethodId;
        if (body != null) {
            if (id == null && body.containsKey("paymentIntentId")) id = body.get("paymentIntentId");
            if (pm == null && body.containsKey("paymentMethodId")) pm = body.get("paymentMethodId");
        }

        log.info("--> [PaymentController] POST /api/payments/confirm-intent for intentId={}, pm={}", id, pm);

        PaymentIntentResponse confirmed = paymentService.confirmPaymentIntent(id, pm);
        log.info("<-- [PaymentController] PaymentIntent confirmed: ID={}, Status: {}", confirmed.getPaymentIntentId(), confirmed.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Stripe payment intent confirmed successfully", confirmed));
    }

    @GetMapping("/saved-cards")
    public ResponseEntity<ApiResponse<List<SavedPaymentMethodDTO>>> getSavedCards() {
        Integer buyerId = CurrentUser.getUserId();
        List<SavedPaymentMethodDTO> cards = paymentService.getSavedPaymentMethods(buyerId);
        return ResponseEntity.ok(ApiResponse.ok("Saved cards retrieved", cards));
    }
}
