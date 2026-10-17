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
public class WebhookAmountMismatchIT {

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
                .email("artist_mismatch_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(artistRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        buyerUser = userAccountRepository.save(UserAccount.builder()
                .email("buyer_mismatch_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(consumerRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        track = trackRepository.save(Track.builder()
                .title("Mismatch Track")
                .artist(artistUser)
                .filePath("/tracks/mismatch.mp3")
                .price(new BigDecimal("25.00"))
                .status("READY")
                .build());

        order = orderRepository.save(Order.builder()
                .buyer(buyerUser)
                .status("PENDING")
                .totalCents(2500L) // 2500 cents expected
                .currency("usd")
                .stripePaymentIntentId("pi_mismatch_test_" + UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .itemType("TRACK")
                .track(track)
                .seller(artistUser)
                .unitPriceCents(2500L)
                .quantity(1)
                .build());
    }

    @Test
    @DisplayName("Amount mismatch leaves order PENDING and grants no entitlement")
    void testAmountMismatchDoesNotFulfillOrder() throws Exception {
        String eventId = "evt_mismatch_" + UUID.randomUUID();
        // Webhook reports only 500 cents instead of 2500 cents
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
                      "amount": 500,
                      "amount_received": 500,
                      "currency": "usd",
                      "status": "succeeded"
                    }
                  }
                }
                """.formatted(eventId, order.getStripePaymentIntentId());

        String sigHeader = StripeTestSigner.computeHeader(payload, stripeProperties.getWebhookSecret());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isOk());

        Order refreshedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(refreshedOrder.getStatus()).isEqualTo("PENDING");
        assertThat(entitlementRepository.existsById(new EntitlementId(buyerUser.getId(), track.getId()))).isFalse();
    }
}
