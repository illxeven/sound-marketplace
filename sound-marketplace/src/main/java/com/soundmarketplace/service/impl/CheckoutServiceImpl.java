package com.soundmarketplace.service.impl;

import com.soundmarketplace.config.StripeProperties;
import com.soundmarketplace.exception.NotFoundException;
import com.soundmarketplace.gateway.CheckoutLineItem;
import com.soundmarketplace.gateway.CheckoutSessionResult;
import com.soundmarketplace.gateway.PaymentIntentResult;
import com.soundmarketplace.gateway.StripeGateway;
import com.soundmarketplace.service.CheckoutService;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.dto.CheckoutItemRequest;
import com.soundmarketplace.sound_marketplace.dto.CheckoutRequest;
import com.soundmarketplace.sound_marketplace.dto.CheckoutResponse;
import com.soundmarketplace.sound_marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutServiceImpl implements CheckoutService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StripeGateway stripeGateway;
    private final StripeProperties stripeProperties;

    @Value("${app.base.url:http://localhost:8080}")
    private String appBaseUrl;

    private static long toCents(BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static class ResolvedItem {
        String itemType;
        Track track;
        Album album;
        Merchandise merchandise;
        UserAccount seller;
        String name;
        long unitPriceCents;
        int quantity;
    }

    private List<ResolvedItem> resolveItems(Integer buyerId, CheckoutRequest request) {
        List<ResolvedItem> resolvedList = new ArrayList<>();

        if (Boolean.TRUE.equals(request.getFromCart())) {
            Cart cart = cartRepository.findByUserId(buyerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty"));
            List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
            if (cartItems.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
            }
            for (CartItem ci : cartItems) {
                if (ci.getTrack() != null) {
                    resolvedList.add(resolveTrack(buyerId, ci.getTrack().getId(), 1));
                } else if (ci.getMerchandise() != null) {
                    resolvedList.add(resolveMerchandise(buyerId, ci.getMerchandise().getId(), ci.getQuantity()));
                }
            }
        } else if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CheckoutItemRequest itemReq : request.getItems()) {
                if (itemReq.getType() == null || itemReq.getId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item type and id are required");
                }
                int qty = (itemReq.getQuantity() != null && itemReq.getQuantity() > 0) ? itemReq.getQuantity() : 1;
                String typeUpper = itemReq.getType().trim().toUpperCase();
                switch (typeUpper) {
                    case "TRACK" -> resolvedList.add(resolveTrack(buyerId, itemReq.getId(), qty));
                    case "ALBUM" -> resolvedList.add(resolveAlbum(buyerId, itemReq.getId(), qty));
                    case "MERCHANDISE" -> resolvedList.add(resolveMerchandise(buyerId, itemReq.getId(), qty));
                    default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown item type: " + itemReq.getType());
                }
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checkout items or fromCart must be provided");
        }

        return resolvedList;
    }

    private ResolvedItem resolveTrack(Integer buyerId, Integer trackId, int qty) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new NotFoundException("Track not found with id: " + trackId));
        if (track.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Track is deleted");
        }
        if (track.getArtist() != null && track.getArtist().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot purchase your own item");
        }
        long cents = toCents(track.getPrice());
        if (cents <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item price must be positive");
        }

        ResolvedItem item = new ResolvedItem();
        item.itemType = "TRACK";
        item.track = track;
        item.seller = track.getArtist();
        item.name = track.getTitle();
        item.unitPriceCents = cents;
        item.quantity = qty;
        return item;
    }

    private ResolvedItem resolveAlbum(Integer buyerId, Integer albumId, int qty) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new NotFoundException("Album not found with id: " + albumId));
        if (album.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Album is deleted");
        }
        if (album.getArtist() != null && album.getArtist().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot purchase your own item");
        }
        long cents = toCents(album.getPrice());
        if (cents <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item price must be positive");
        }

        ResolvedItem item = new ResolvedItem();
        item.itemType = "ALBUM";
        item.album = album;
        item.seller = album.getArtist();
        item.name = album.getTitle();
        item.unitPriceCents = cents;
        item.quantity = qty;
        return item;
    }

    private ResolvedItem resolveMerchandise(Integer buyerId, Integer merchId, int qty) {
        Merchandise merch = merchandiseRepository.findById(merchId)
                .orElseThrow(() -> new NotFoundException("Merchandise not found with id: " + merchId));
        if (merch.getSeller() != null && merch.getSeller().getId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot purchase your own item");
        }
        if (merch.getStockQuantity() == null || merch.getStockQuantity() < qty) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Merchandise is out of stock");
        }
        long cents = toCents(merch.getPrice());
        if (cents <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item price must be positive");
        }

        ResolvedItem item = new ResolvedItem();
        item.itemType = "MERCHANDISE";
        item.merchandise = merch;
        item.seller = merch.getSeller();
        item.name = merch.getTitle();
        item.unitPriceCents = cents;
        item.quantity = qty;
        return item;
    }

    @Override
    @Transactional
    public CheckoutResponse checkout(Integer buyerId, CheckoutRequest request) {
        UserAccount buyer = userAccountRepository.findById(buyerId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + buyerId));

        List<ResolvedItem> resolvedItems = resolveItems(buyerId, request);

        long totalCents = 0L;
        for (ResolvedItem ri : resolvedItems) {
            totalCents += ri.unitPriceCents * ri.quantity;
        }

        if (totalCents < 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum checkout amount is 50 cents");
        }

        Order order = Order.builder()
                .buyer(buyer)
                .status("PENDING")
                .totalCents(totalCents)
                .currency("usd")
                .build();
        order = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (ResolvedItem ri : resolvedItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .itemType(ri.itemType)
                    .track(ri.track)
                    .album(ri.album)
                    .merchandise(ri.merchandise)
                    .seller(ri.seller)
                    .unitPriceCents(ri.unitPriceCents)
                    .quantity(ri.quantity)
                    .build();
            orderItems.add(orderItemRepository.save(orderItem));
        }
        order.setItems(orderItems);

        String customerId = stripeGateway.createOrGetCustomer(buyer.getEmail(), buyer.getEmail());
        String ephemeralKey = customerId != null ? stripeGateway.createEphemeralKey(customerId) : null;

        Map<String, String> metadata = Map.of(
                "orderId", String.valueOf(order.getId()),
                "buyerId", String.valueOf(buyerId)
        );

        PaymentIntentResult piResult = stripeGateway.createPaymentIntent(
                totalCents,
                "usd",
                customerId,
                metadata,
                "order-" + order.getId()
        );

        order.setStripePaymentIntentId(piResult.getPaymentIntentId());
        orderRepository.save(order);

        log.info("EVENT order.created orderId={} totalCents={} items={} clientAmountIgnored=true",
                order.getId(), totalCents, orderItems.size());

        return CheckoutResponse.builder()
                .orderId(order.getId())
                .clientSecret(piResult.getClientSecret())
                .customerId(customerId)
                .ephemeralKey(ephemeralKey)
                .build();
    }

    @Override
    @Transactional
    public CheckoutResponse createSession(Integer buyerId, CheckoutRequest request) {
        UserAccount buyer = userAccountRepository.findById(buyerId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + buyerId));

        List<ResolvedItem> resolvedItems = resolveItems(buyerId, request);

        long totalCents = 0L;
        List<CheckoutLineItem> lineItems = new ArrayList<>();
        for (ResolvedItem ri : resolvedItems) {
            totalCents += ri.unitPriceCents * ri.quantity;
            lineItems.add(CheckoutLineItem.builder()
                    .name(ri.name)
                    .unitAmountCents(ri.unitPriceCents)
                    .currency("usd")
                    .quantity(ri.quantity)
                    .build());
        }

        if (totalCents < 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum checkout amount is 50 cents");
        }

        Order order = Order.builder()
                .buyer(buyer)
                .status("PENDING")
                .totalCents(totalCents)
                .currency("usd")
                .build();
        order = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (ResolvedItem ri : resolvedItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .itemType(ri.itemType)
                    .track(ri.track)
                    .album(ri.album)
                    .merchandise(ri.merchandise)
                    .seller(ri.seller)
                    .unitPriceCents(ri.unitPriceCents)
                    .quantity(ri.quantity)
                    .build();
            orderItems.add(orderItemRepository.save(orderItem));
        }
        order.setItems(orderItems);

        String successUrl = (request.getSuccessUrl() != null && !request.getSuccessUrl().isBlank())
                ? request.getSuccessUrl()
                : appBaseUrl + "/checkout/success?orderId=" + order.getId();
        String cancelUrl = (request.getCancelUrl() != null && !request.getCancelUrl().isBlank())
                ? request.getCancelUrl()
                : appBaseUrl + "/checkout/cancel?orderId=" + order.getId();

        Map<String, String> metadata = Map.of(
                "orderId", String.valueOf(order.getId()),
                "buyerId", String.valueOf(buyerId)
        );

        CheckoutSessionResult sessionResult = stripeGateway.createCheckoutSession(
                lineItems,
                "usd",
                successUrl,
                cancelUrl,
                metadata
        );

        log.info("EVENT order.created orderId={} totalCents={} items={} clientAmountIgnored=true",
                order.getId(), totalCents, orderItems.size());

        return CheckoutResponse.builder()
                .orderId(order.getId())
                .url(sessionResult.getUrl())
                .build();
    }
}
