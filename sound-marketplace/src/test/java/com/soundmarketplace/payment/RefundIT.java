package com.soundmarketplace.payment;

import com.soundmarketplace.config.StripeProperties;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class RefundIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StripeProperties stripeProperties;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private EntitlementRepository entitlementRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    private UserAccount buyerUser;
    private UserAccount artistUser;
    private Track track;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));
        Role artistRole = roleRepository.findByRoleNameIgnoreCase("music_artist")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("music_artist").build()));

        artistUser = userAccountRepository.save(UserAccount.builder()
                .email("artist_refund_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(artistRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        buyerUser = userAccountRepository.save(UserAccount.builder()
                .email("buyer_refund_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(consumerRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        track = trackRepository.save(Track.builder()
                .title("Refundable Track")
                .artist(artistUser)
                .filePath("/tracks/refund.mp3")
                .price(new BigDecimal("15.00"))
                .status("READY")
                .build());

        order = orderRepository.save(Order.builder()
                .buyer(buyerUser)
                .status("PAID")
                .totalCents(1500L)
                .currency("usd")
                .stripePaymentIntentId("pi_refund_test_" + UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .paidAt(LocalDateTime.now())
                .build());

        orderItem = orderItemRepository.save(OrderItem.builder()
                .order(order)
                .itemType("TRACK")
                .track(track)
                .seller(artistUser)
                .unitPriceCents(1500L)
                .quantity(1)
                .build());

        // Grant initial entitlement
        entitlementRepository.save(Entitlement.builder()
                .id(new EntitlementId(buyerUser.getId(), track.getId()))
                .user(buyerUser)
                .track(track)
                .orderItem(orderItem)
                .grantedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("charge.refunded webhook transitions order to REFUNDED and revokes entitlements")
    void testRefundRevokesEntitlements() throws Exception {
        assertThat(entitlementRepository.existsById(new EntitlementId(buyerUser.getId(), track.getId()))).isTrue();

        String eventId = "evt_refund_" + UUID.randomUUID();
        String payload = """
                {
                  "id": "%s",
                  "object": "event",
                  "api_version": "2024-06-20",
                  "type": "charge.refunded",
                  "data": {
                    "object": {
                      "id": "ch_refund_%s",
                      "object": "charge",
                      "amount": 1500,
                      "amount_refunded": 1500,
                      "payment_intent": "%s",
                      "refunded": true,
                      "currency": "usd"
                    }
                  }
                }
                """.formatted(eventId, UUID.randomUUID(), order.getStripePaymentIntentId());

        String sigHeader = StripeTestSigner.computeHeader(payload, stripeProperties.getWebhookSecret());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isOk());

        Order refundedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(refundedOrder.getStatus()).isEqualTo("REFUNDED");

        // Verify entitlement was revoked
        assertThat(entitlementRepository.existsById(new EntitlementId(buyerUser.getId(), track.getId()))).isFalse();
    }
}
