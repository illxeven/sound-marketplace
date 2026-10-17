package com.soundmarketplace.service.impl;

import com.soundmarketplace.config.StripeProperties;
import com.soundmarketplace.exception.NotFoundException;
import com.soundmarketplace.service.OrderService;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.dto.OrderDTO;
import com.soundmarketplace.sound_marketplace.dto.OrderItemDTO;
import com.soundmarketplace.sound_marketplace.dto.TrackDTO;
import com.soundmarketplace.sound_marketplace.repository.*;
import com.soundmarketplace.util.CurrentUser;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EntitlementRepository entitlementRepository;
    private final StripeEventRepository stripeEventRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final TrackRepository trackRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StripeProperties stripeProperties;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getMyOrders(Integer buyerId, Pageable pageable) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId, Integer currentUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));
        CurrentUser.requireOwnerOrAdmin(order.getBuyer().getId());
        return mapToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrackDTO> getLibrary(Integer userId, Pageable pageable) {
        return entitlementRepository.findTracksByUserId(userId, pageable)
                .map(this::mapTrackToDTO);
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
        } catch (Exception e) {
            log.warn("Webhook signature verification failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }

        // Idempotency check via native insert ignore
        int inserted = stripeEventRepository.insertIgnore(event.getId(), event.getType());
        if (inserted == 0) {
            log.info("EVENT webhook.duplicate eventId={}", event.getId());
            return;
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);
        if (stripeObject == null) {
            try {
                stripeObject = dataObjectDeserializer.deserializeUnsafe();
            } catch (Exception e) {
                log.warn("Failed to deserialize StripeObject: {}", e.getMessage());
            }
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                if (stripeObject instanceof PaymentIntent intent) {
                    handlePaymentIntentSucceeded(intent);
                }
            }
            case "checkout.session.completed" -> {
                if (stripeObject instanceof Session session) {
                    handleCheckoutSessionCompleted(session);
                }
            }
            case "payment_intent.payment_failed" -> {
                if (stripeObject instanceof PaymentIntent intent) {
                    handlePaymentIntentFailed(intent);
                }
            }
            case "charge.refunded" -> {
                if (stripeObject instanceof Charge charge) {
                    handleChargeRefunded(charge);
                }
            }
            default -> log.debug("Unhandled webhook event type: {}", event.getType());
        }
    }

    private void handlePaymentIntentSucceeded(PaymentIntent intent) {
        Order order = findOrderByIntent(intent.getId(), intent.getMetadata() != null ? intent.getMetadata().get("orderId") : null);
        if (order == null) {
            log.warn("Order not found for succeeded PaymentIntent: {}", intent.getId());
            return;
        }
        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            return;
        }

        long receivedCents = intent.getAmountReceived() != null ? intent.getAmountReceived() : (intent.getAmount() != null ? intent.getAmount() : 0L);
        String currency = intent.getCurrency();

        if (receivedCents != order.getTotalCents() || (currency != null && !currency.equalsIgnoreCase(order.getCurrency()))) {
            log.info("EVENT order.amount_mismatch orderId={} received={} expected={}", order.getId(), receivedCents, order.getTotalCents());
            return;
        }

        fulfillOrder(order, intent.getId());
    }

    private void handleCheckoutSessionCompleted(Session session) {
        String orderIdStr = session.getMetadata() != null ? session.getMetadata().get("orderId") : null;
        Order order = findOrderByIntent(session.getPaymentIntent(), orderIdStr);
        if (order == null) {
            log.warn("Order not found for completed checkout session: {}", session.getId());
            return;
        }
        if ("PAID".equalsIgnoreCase(order.getStatus())) {
            return;
        }

        long totalReceived = session.getAmountTotal() != null ? session.getAmountTotal() : 0L;
        String currency = session.getCurrency();

        if (totalReceived != order.getTotalCents() || (currency != null && !currency.equalsIgnoreCase(order.getCurrency()))) {
            log.info("EVENT order.amount_mismatch orderId={} received={} expected={}", order.getId(), totalReceived, order.getTotalCents());
            return;
        }

        fulfillOrder(order, session.getPaymentIntent() != null ? session.getPaymentIntent() : order.getStripePaymentIntentId());
    }

    private void fulfillOrder(Order order, String paymentIntentId) {
        boolean oversold = false;
        UserAccount buyer = order.getBuyer();

        for (OrderItem item : order.getItems()) {
            if ("TRACK".equalsIgnoreCase(item.getItemType()) && item.getTrack() != null) {
                EntitlementId entId = new EntitlementId(buyer.getId(), item.getTrack().getId());
                if (!entitlementRepository.existsById(entId)) {
                    Entitlement entitlement = Entitlement.builder()
                            .id(entId)
                            .user(buyer)
                            .track(item.getTrack())
                            .orderItem(item)
                            .grantedAt(LocalDateTime.now())
                            .build();
                    entitlementRepository.save(entitlement);
                    log.info("EVENT entitlement.granted userId={} trackId={} orderItemId={}",
                            buyer.getId(), item.getTrack().getId(), item.getId());
                }
            } else if ("ALBUM".equalsIgnoreCase(item.getItemType()) && item.getAlbum() != null) {
                List<Track> albumTracks = trackRepository.findByAlbumIdAndDeletedAtIsNull(item.getAlbum().getId());
                for (Track track : albumTracks) {
                    EntitlementId entId = new EntitlementId(buyer.getId(), track.getId());
                    if (!entitlementRepository.existsById(entId)) {
                        Entitlement entitlement = Entitlement.builder()
                                .id(entId)
                                .user(buyer)
                                .track(track)
                                .orderItem(item)
                                .grantedAt(LocalDateTime.now())
                                .build();
                        entitlementRepository.save(entitlement);
                        log.info("EVENT entitlement.granted userId={} trackId={} orderItemId={}",
                                buyer.getId(), track.getId(), item.getId());
                    }
                }
            } else if ("MERCHANDISE".equalsIgnoreCase(item.getItemType()) && item.getMerchandise() != null) {
                int updated = merchandiseRepository.decrementStockIfAvailable(item.getMerchandise().getId(), item.getQuantity());
                if (updated == 0) {
                    oversold = true;
                    log.info("EVENT order.oversold orderId={} merchandiseId={}", order.getId(), item.getMerchandise().getId());
                }
            }
        }

        if (paymentIntentId != null && !paymentIntentId.isBlank()) {
            order.setStripePaymentIntentId(paymentIntentId);
        }

        if (oversold) {
            order.setStatus("NEEDS_REFUND");
        } else {
            order.setStatus("PAID");
            order.setPaidAt(LocalDateTime.now());
            log.info("EVENT order.paid orderId={} totalCents={}", order.getId(), order.getTotalCents());

            // Clear buyer cart
            cartRepository.findByUserId(buyer.getId()).ifPresent(c -> cartItemRepository.deleteByCartId(c.getId()));
        }

        orderRepository.save(order);
    }

    private void handlePaymentIntentFailed(PaymentIntent intent) {
        Order order = findOrderByIntent(intent.getId(), intent.getMetadata() != null ? intent.getMetadata().get("orderId") : null);
        if (order != null) {
            order.setStatus("FAILED");
            orderRepository.save(order);
            log.info("EVENT order.failed orderId={}", order.getId());
        }
    }

    private void handleChargeRefunded(Charge charge) {
        String piId = charge.getPaymentIntent();
        Order order = findOrderByIntent(piId, charge.getMetadata() != null ? charge.getMetadata().get("orderId") : null);
        if (order != null) {
            order.setStatus("REFUNDED");
            orderRepository.save(order);

            List<Long> orderItemIds = order.getItems().stream().map(OrderItem::getId).toList();
            if (!orderItemIds.isEmpty()) {
                entitlementRepository.deleteAllByOrderItemIdIn(orderItemIds);
            }
            log.info("EVENT entitlement.revoked orderId={}", order.getId());
        }
    }

    private Order findOrderByIntent(String paymentIntentId, String orderIdStr) {
        if (paymentIntentId != null && !paymentIntentId.isBlank()) {
            Optional<Order> opt = orderRepository.findByStripePaymentIntentId(paymentIntentId);
            if (opt.isPresent()) return opt.get();
        }
        if (orderIdStr != null && !orderIdStr.isBlank()) {
            try {
                Long orderId = Long.valueOf(orderIdStr);
                return orderRepository.findById(orderId).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private OrderDTO mapToDTO(Order order) {
        List<OrderItemDTO> itemDTOs = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                String title = null;
                if (oi.getTrack() != null) title = oi.getTrack().getTitle();
                else if (oi.getAlbum() != null) title = oi.getAlbum().getTitle();
                else if (oi.getMerchandise() != null) title = oi.getMerchandise().getTitle();

                itemDTOs.add(OrderItemDTO.builder()
                        .id(oi.getId())
                        .itemType(oi.getItemType())
                        .trackId(oi.getTrack() != null ? oi.getTrack().getId() : null)
                        .albumId(oi.getAlbum() != null ? oi.getAlbum().getId() : null)
                        .merchandiseId(oi.getMerchandise() != null ? oi.getMerchandise().getId() : null)
                        .title(title)
                        .unitPriceCents(oi.getUnitPriceCents())
                        .quantity(oi.getQuantity())
                        .build());
            }
        }

        return OrderDTO.builder()
                .id(order.getId())
                .buyerId(order.getBuyer() != null ? order.getBuyer().getId() : null)
                .status(order.getStatus())
                .totalCents(order.getTotalCents())
                .currency(order.getCurrency())
                .stripePaymentIntentId(order.getStripePaymentIntentId())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .items(itemDTOs)
                .build();
    }

    private TrackDTO mapTrackToDTO(Track track) {
        return TrackDTO.builder()
                .id(track.getId())
                .artistId(track.getArtist() != null ? track.getArtist().getId() : null)
                .artistName(track.getArtistName())
                .albumId(track.getAlbum() != null ? track.getAlbum().getId() : null)
                .albumTitle(track.getAlbum() != null ? track.getAlbum().getTitle() : null)
                .title(track.getTitle())
                .durationSeconds(track.getDurationSeconds())
                .genre(track.getGenre() != null ? track.getGenre().getName() : null)
                .price(track.getPrice())
                .createdAt(track.getCreatedAt())
                .build();
    }
}
