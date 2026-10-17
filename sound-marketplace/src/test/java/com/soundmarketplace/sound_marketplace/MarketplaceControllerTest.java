package com.soundmarketplace.sound_marketplace;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.MerchandiseType;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.CreateMerchandiseRequest;
import com.soundmarketplace.sound_marketplace.dto.CreatePaymentRequest;
import com.soundmarketplace.sound_marketplace.repository.MerchandiseRepository;
import com.soundmarketplace.sound_marketplace.repository.MerchandiseCategoryRepository;
import com.soundmarketplace.sound_marketplace.repository.MerchandiseTypeRepository;
import com.soundmarketplace.sound_marketplace.repository.PaymentHistoryRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchandiseRepository merchandiseRepository;

    @Autowired
    private MerchandiseTypeRepository merchandiseTypeRepository;

    @Autowired
    private MerchandiseCategoryRepository merchandiseCategoryRepository;

    @Autowired
    private com.soundmarketplace.sound_marketplace.repository.MerchandiseGroupTypeRepository merchandiseGroupTypeRepository;

    @Autowired
    private com.soundmarketplace.sound_marketplace.repository.RoleRepository roleRepository;

    @Autowired
    private com.soundmarketplace.sound_marketplace.repository.MerchandiseReviewRepository merchandiseReviewRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount testSeller;
    private UserAccount testBuyer;
    private MerchandiseType testType;
    private com.soundmarketplace.sound_marketplace.domain.MerchandiseCategory testCategory;

    @BeforeEach
    void setUp() {
        merchandiseReviewRepository.deleteAll();
        paymentHistoryRepository.deleteAll();
        merchandiseRepository.deleteAll();

        com.soundmarketplace.sound_marketplace.domain.Role artistRole = roleRepository.findByRoleNameIgnoreCase("MUSIC_ARTIST")
                .orElseGet(() -> roleRepository.save(com.soundmarketplace.sound_marketplace.domain.Role.builder().roleName("MUSIC_ARTIST").build()));

        testSeller = userAccountRepository.findByEmail("seller_mkt@soundmarketplace.com")
                .orElseGet(() -> userAccountRepository.save(UserAccount.builder()
                        .email("seller_mkt@soundmarketplace.com")
                        .passwordHash("password")
                        .isActive(true)
                        .roles(java.util.Set.of(artistRole))
                        .build()));
        if (testSeller.getRoles() == null || testSeller.getRoles().isEmpty()) {
            testSeller.setRoles(java.util.Set.of(artistRole));
            testSeller = userAccountRepository.save(testSeller);
        }

        testBuyer = userAccountRepository.findByEmail("buyer_mkt@soundmarketplace.com")
                .orElseGet(() -> userAccountRepository.save(UserAccount.builder()
                        .email("buyer_mkt@soundmarketplace.com")
                        .passwordHash("password")
                        .isActive(true)
                        .build()));

        testType = merchandiseTypeRepository.findByTypeNameIgnoreCase("Sample Pack")
                .orElseGet(() -> merchandiseTypeRepository.save(MerchandiseType.builder()
                        .typeName("Sample Pack")
                        .build()));

        com.soundmarketplace.sound_marketplace.domain.MerchandiseGroupType groupType = merchandiseGroupTypeRepository.findAll().stream().findFirst()
                .orElseGet(() -> merchandiseGroupTypeRepository.save(com.soundmarketplace.sound_marketplace.domain.MerchandiseGroupType.builder().name("Physical Products").build()));

        testCategory = merchandiseCategoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> merchandiseCategoryRepository.save(com.soundmarketplace.sound_marketplace.domain.MerchandiseCategory.builder().name("Physical Merch").groupType(groupType).build()));
    }

    @Test
    @DisplayName("POST & GET /api/merchandise creates and lists merchandise")
    void testCreateAndGetMerchandise() throws Exception {
        CreateMerchandiseRequest request = CreateMerchandiseRequest.builder()
                .sellerId(testSeller.getId())
                .categoryId(testCategory.getId())
                .merchandiseTypeId(testType.getId())
                .title("808 Drum Kit Deluxe")
                .price(BigDecimal.valueOf(29.99))
                .build();

        mockMvc.perform(post("/api/merchandise")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("808 Drum Kit Deluxe")))
                .andExpect(jsonPath("$.data.merchandiseTypeName", is("Sample Pack")));

        mockMvc.perform(get("/api/merchandise"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("POST & GET /api/payments processes payment and retrieves purchase history")
    void testProcessPaymentAndHistory() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .buyerId(testBuyer.getId())
                .stripeChargeId("ch_3M4ABC123XYZ")
                .amount(BigDecimal.valueOf(49.99))
                .build();

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.stripeChargeId", is("ch_3M4ABC123XYZ")));

        mockMvc.perform(get("/api/payments/buyer/" + testBuyer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].amount", is(49.99)));
    }

    @Test
    @DisplayName("POST & GET /api/merchandise/{id}/reviews successfully adds review without requiring merchandiseId in body")
    void testAddMerchandiseReview() throws Exception {
        com.soundmarketplace.sound_marketplace.domain.Merchandise merch = merchandiseRepository.save(
                com.soundmarketplace.sound_marketplace.domain.Merchandise.builder()
                        .seller(testSeller)
                        .merchandiseType(testType)
                        .category(testCategory)
                        .title("Vintage Synthesizer T-Shirt")
                        .price(BigDecimal.valueOf(25.00))
                        .build()
        );

        // Body with NO merchandiseId: should succeed because ID is taken from URL path
        mockMvc.perform(post("/api/merchandise/" + merch.getId() + "/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                        "userId", testBuyer.getId(),
                        "rating", 5,
                        "comment", "Outstanding quality material and fit!"
                ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.merchandiseId", is(merch.getId())))
                .andExpect(jsonPath("$.data.rating", is(5)))
                .andExpect(jsonPath("$.data.comment", is("Outstanding quality material and fit!")));

        // Verify GET /api/merchandise/{id}/reviews returns the review
        mockMvc.perform(get("/api/merchandise/" + merch.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].comment", is("Outstanding quality material and fit!")));
    }
}
