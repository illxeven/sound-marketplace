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
public class WebhookIdempotencyIT {

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

    @BeforeEach
    void setUp() {
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));
        Role artistRole = roleRepository.findByRoleNameIgnoreCase("music_artist")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("music_artist").build()));

        artistUser = userAccountRepository.save(UserAccount.builder()
                .email("artist_idem_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(artistRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        buyerUser = userAccountRepository.save(UserAccount.builder()
                .email("buyer_idem_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(consumerRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        track = trackRepository.save(Track.builder()
                .title("Idempotency Track")
                .artist(artistUser)
                .filePath("/tracks/idem.mp3")
                .price(new BigDecimal("20.00"))
                .status("READY")
                .build());

        order = orderRepository.save(Order.builder()
                .buyer(buyerUser)
                .status("PENDING")
                .totalCents(2000L)
                .currency("usd")
                .stripePaymentIntentId("pi_idem_test_" + UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .itemType("TRACK")
                .track(track)
                .seller(artistUser)
                .unitPriceCents(2000L)
                .quantity(1)
                .build());
    }

    @Test
    @DisplayName("Duplicate webhook delivery returns 200 OK without re-granting entitlements")
    void testDuplicateWebhookIdempotent() throws Exception {
        String eventId = "evt_idem_" + UUID.randomUUID();
        String payload = """
                {
                  "id": "%s",
                  "object": "event",
                  "api_version": "2024-06-20",
                  "type": "payment_intent.succeeded",
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "payment_intent",
                      "amount": 2000,
                      "amount_received": 2000,
                      "currency": "usd",
                      "status": "succeeded"
                    }
                  }
                }
                """.formatted(eventId, order.getStripePaymentIntentId());

        String sigHeader = StripeTestSigner.computeHeader(payload, stripeProperties.getWebhookSecret());

        // First delivery: should succeed and transition order to PAID
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isOk());

        Order paidOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(paidOrder.getStatus()).isEqualTo("PAID");
        assertThat(entitlementRepository.existsById(new EntitlementId(buyerUser.getId(), track.getId()))).isTrue();

        long entitlementCountBefore = entitlementRepository.count();

        // Second delivery (duplicate): should return 200 and not change state or duplicate entitlements
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isOk());

        Order orderAfter = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(orderAfter.getStatus()).isEqualTo("PAID");
        assertThat(entitlementRepository.count()).isEqualTo(entitlementCountBefore);
    }
}
