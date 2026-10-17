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
public class AlbumEntitlementIT {

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
    private AlbumRepository albumRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    private UserAccount buyerUser;
    private UserAccount artistUser;
    private Album album;
    private Track track1;
    private Track track2;
    private Track track3;
    private Order order;

    @BeforeEach
    void setUp() {
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));
        Role artistRole = roleRepository.findByRoleNameIgnoreCase("music_artist")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("music_artist").build()));

        artistUser = userAccountRepository.save(UserAccount.builder()
                .email("artist_album_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(artistRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        buyerUser = userAccountRepository.save(UserAccount.builder()
                .email("buyer_album_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(consumerRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        album = albumRepository.save(Album.builder()
                .title("Full Length LP")
                .artist(artistUser)
                .price(new BigDecimal("30.00"))
                .build());

        track1 = trackRepository.save(Track.builder()
                .title("LP Track 1")
                .artist(artistUser)
                .album(album)
                .filePath("/tracks/lp1.mp3")
                .price(new BigDecimal("10.00"))
                .status("READY")
                .build());

        track2 = trackRepository.save(Track.builder()
                .title("LP Track 2")
                .artist(artistUser)
                .album(album)
                .filePath("/tracks/lp2.mp3")
                .price(new BigDecimal("10.00"))
                .status("READY")
                .build());

        track3 = trackRepository.save(Track.builder()
                .title("LP Track 3")
                .artist(artistUser)
                .album(album)
                .filePath("/tracks/lp3.mp3")
                .price(new BigDecimal("10.00"))
                .status("READY")
                .build());

        order = orderRepository.save(Order.builder()
                .buyer(buyerUser)
                .status("PENDING")
                .totalCents(3000L)
                .currency("usd")
                .stripePaymentIntentId("pi_album_test_" + UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .itemType("ALBUM")
                .album(album)
                .seller(artistUser)
                .unitPriceCents(3000L)
                .quantity(1)
                .build());
    }

    @Test
    @DisplayName("Album purchase grants an entitlement for every track in the album")
    void testAlbumPurchaseGrantsAllTracks() throws Exception {
        String eventId = "evt_album_" + UUID.randomUUID();
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
                      "amount": 3000,
                      "amount_received": 3000,
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

        Order paidOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(paidOrder.getStatus()).isEqualTo("PAID");

        // Verify all 3 tracks in the album were granted to the buyer
        assertThat(entitlementRepository.existsById(new EntitlementId(buyerUser.getId(), track1.getId()))).isTrue();
        assertThat(entitlementRepository.existsById(new EntitlementId(buyerUser.getId(), track2.getId()))).isTrue();
        assertThat(entitlementRepository.existsById(new EntitlementId(buyerUser.getId(), track3.getId()))).isTrue();
    }
}
