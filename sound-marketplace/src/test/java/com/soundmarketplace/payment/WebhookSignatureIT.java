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
public class WebhookSignatureIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StripeProperties stripeProperties;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

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
                .email("artist_sig_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(artistRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        buyerUser = userAccountRepository.save(UserAccount.builder()
                .email("buyer_sig_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(consumerRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        track = trackRepository.save(Track.builder()
                .title("Signature Test Track")
                .artist(artistUser)
                .filePath("/tracks/sig.mp3")
                .price(new BigDecimal("10.00"))
                .status("READY")
                .build());

        order = orderRepository.save(Order.builder()
                .buyer(buyerUser)
                .status("PENDING")
                .totalCents(1000L)
                .currency("usd")
                .stripePaymentIntentId("pi_sig_test_" + UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .itemType("TRACK")
                .track(track)
                .seller(artistUser)
                .unitPriceCents(1000L)
                .quantity(1)
                .build());
    }

    @Test
    @DisplayName("Webhook with bad signature returns 400 Bad Request and order is unchanged")
    void testBadSignatureReturns400() throws Exception {
        String payload = """
                {
                  "id": "evt_bad_sig_1",
                  "object": "event",
                  "api_version": "2024-06-20",
                  "type": "payment_intent.succeeded",
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "payment_intent",
                      "amount": 1000,
                      "amount_received": 1000,
                      "currency": "usd",
                      "status": "succeeded"
                    }
                  }
                }
                """.formatted(order.getStripePaymentIntentId());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=12345,v1=bad_hex_signature")
                        .content(payload))
                .andExpect(status().isBadRequest());

        Order refreshed = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Webhook with valid signature returns 200 OK and order transitions to PAID")
    void testValidSignatureFulfillsOrder() throws Exception {
        String payload = """
                {
                  "id": "evt_valid_sig_%s",
                  "object": "event",
                  "api_version": "2024-06-20",
                  "type": "payment_intent.succeeded",
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "payment_intent",
                      "amount": 1000,
                      "amount_received": 1000,
                      "currency": "usd",
                      "status": "succeeded"
                    }
                  }
                }
                """.formatted(UUID.randomUUID(), order.getStripePaymentIntentId());

        String sigHeader = StripeTestSigner.computeHeader(payload, stripeProperties.getWebhookSecret());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sigHeader)
                        .content(payload))
                .andExpect(status().isOk());

        Order refreshed = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("PAID");
    }
}
