package com.soundmarketplace.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundmarketplace.security.JwtTestTokens;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.dto.CheckoutItemRequest;
import com.soundmarketplace.sound_marketplace.dto.CheckoutRequest;
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
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class CheckoutPricingIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtTestTokens jwtTestTokens;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private MerchandiseRepository merchandiseRepository;

    @Autowired
    private MerchandiseTypeRepository merchandiseTypeRepository;

    @Autowired
    private MerchandiseCategoryRepository merchandiseCategoryRepository;

    @Autowired
    private MerchandiseGroupTypeRepository merchandiseGroupTypeRepository;

    @Autowired
    private OrderRepository orderRepository;

    private UserAccount artistUser;
    private UserAccount buyerUser;
    private Track paidTrack;
    private Track freeTrack;
    private Merchandise outOfStockMerch;
    private Merchandise inStockMerch;

    @BeforeEach
    void setUp() {
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));
        Role artistRole = roleRepository.findByRoleNameIgnoreCase("music_artist")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("music_artist").build()));

        artistUser = userAccountRepository.save(UserAccount.builder()
                .email("artist_pricing_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("$2a$12$dummyhashdummyhashdummyhashdummyhashdummyhash")
                .roles(Set.of(artistRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        buyerUser = userAccountRepository.save(UserAccount.builder()
                .email("buyer_pricing_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("$2a$12$dummyhashdummyhashdummyhashdummyhashdummyhash")
                .roles(Set.of(consumerRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        paidTrack = trackRepository.save(Track.builder()
                .title("Paid Track")
                .artist(artistUser)
                .price(new BigDecimal("15.00")) // 1500 cents
                .filePath("/tracks/paid.mp3")
                .status("READY")
                .build());

        freeTrack = trackRepository.save(Track.builder()
                .title("Free Track")
                .artist(artistUser)
                .price(BigDecimal.ZERO)
                .filePath("/tracks/free.mp3")
                .status("READY")
                .build());

        MerchandiseGroupType groupType = merchandiseGroupTypeRepository.findAll().stream().findFirst()
                .orElseGet(() -> merchandiseGroupTypeRepository.save(MerchandiseGroupType.builder().name("Physical_" + System.currentTimeMillis()).build()));
        MerchandiseCategory cat = merchandiseCategoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> merchandiseCategoryRepository.save(MerchandiseCategory.builder().name("Apparel_" + System.currentTimeMillis()).groupType(groupType).build()));
        MerchandiseType type = merchandiseTypeRepository.findAll().stream().findFirst()
                .orElseGet(() -> merchandiseTypeRepository.save(MerchandiseType.builder().typeName("T-Shirt_" + System.currentTimeMillis()).build()));

        outOfStockMerch = merchandiseRepository.save(Merchandise.builder()
                .title("Sold Out Hoodie")
                .seller(artistUser)
                .category(cat)
                .merchandiseType(type)
                .price(new BigDecimal("30.00"))
                .stockQuantity(0)
                .build());

        inStockMerch = merchandiseRepository.save(Merchandise.builder()
                .title("Available Cap")
                .seller(artistUser)
                .category(cat)
                .merchandiseType(type)
                .price(new BigDecimal("20.00"))
                .stockQuantity(5)
                .build());
    }

    @Test
    @DisplayName("Amounts in request body or query are ignored; total equals DB prices * quantity")
    void testPricingDerivedStrictlyFromDatabase() throws Exception {
        CheckoutItemRequest item = CheckoutItemRequest.builder()
                .type("TRACK")
                .id(paidTrack.getId())
                .quantity(2)
                .build();

        CheckoutRequest request = CheckoutRequest.builder()
                .items(List.of(item))
                .build();

        String responseJson = mockMvc.perform(post("/api/checkout")
                        .header("Authorization", jwtTestTokens.consumer(buyerUser.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").isNotEmpty())
                .andExpect(jsonPath("$.data.clientSecret").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(responseJson).get("data").get("orderId").asLong();
        Order order = orderRepository.findById(orderId).orElseThrow();

        // 15.00 * 2 = 30.00 -> 3000 cents
        assertThat(order.getTotalCents()).isEqualTo(3000L);
        assertThat(order.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Purchasing own item is rejected with 400 Bad Request")
    void testCannotBuyOwnTrack() throws Exception {
        CheckoutItemRequest item = CheckoutItemRequest.builder()
                .type("TRACK")
                .id(paidTrack.getId())
                .quantity(1)
                .build();

        CheckoutRequest request = CheckoutRequest.builder()
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/checkout")
                        .header("Authorization", jwtTestTokens.artist(artistUser.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("cannot purchase your own item")));
    }

    @Test
    @DisplayName("Purchasing free item is rejected with 400 Bad Request")
    void testFreeItemRejected() throws Exception {
        CheckoutItemRequest item = CheckoutItemRequest.builder()
                .type("TRACK")
                .id(freeTrack.getId())
                .quantity(1)
                .build();

        CheckoutRequest request = CheckoutRequest.builder()
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/checkout")
                        .header("Authorization", jwtTestTokens.consumer(buyerUser.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("price must be positive")));
    }

    @Test
    @DisplayName("Purchasing out of stock item is rejected with 409 Conflict")
    void testOutOfStockItemRejected() throws Exception {
        CheckoutItemRequest item = CheckoutItemRequest.builder()
                .type("MERCHANDISE")
                .id(outOfStockMerch.getId())
                .quantity(1)
                .build();

        CheckoutRequest request = CheckoutRequest.builder()
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/checkout")
                        .header("Authorization", jwtTestTokens.consumer(buyerUser.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("out of stock")));
    }
}
