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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class StockConcurrencyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StripeProperties stripeProperties;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MerchandiseRepository merchandiseRepository;

    @Autowired
    private MerchandiseTypeRepository merchandiseTypeRepository;

    @Autowired
    private MerchandiseCategoryRepository merchandiseCategoryRepository;

    @Autowired
    private MerchandiseGroupTypeRepository merchandiseGroupTypeRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    private UserAccount sellerUser;
    private UserAccount buyerUser1;
    private UserAccount buyerUser2;
    private Merchandise limitedMerch;
    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));
        Role artistRole = roleRepository.findByRoleNameIgnoreCase("music_artist")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("music_artist").build()));

        sellerUser = userAccountRepository.save(UserAccount.builder()
                .email("seller_conc_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(artistRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        buyerUser1 = userAccountRepository.save(UserAccount.builder()
                .email("buyer1_conc_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(consumerRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        buyerUser2 = userAccountRepository.save(UserAccount.builder()
                .email("buyer2_conc_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hash")
                .roles(Set.of(consumerRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        MerchandiseGroupType groupType = merchandiseGroupTypeRepository.findAll().stream().findFirst()
                .orElseGet(() -> merchandiseGroupTypeRepository.save(MerchandiseGroupType.builder().name("Physical_" + System.currentTimeMillis()).build()));
        MerchandiseCategory cat = merchandiseCategoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> merchandiseCategoryRepository.save(MerchandiseCategory.builder().name("Vinyl_" + System.currentTimeMillis()).groupType(groupType).build()));
        MerchandiseType type = merchandiseTypeRepository.findAll().stream().findFirst()
                .orElseGet(() -> merchandiseTypeRepository.save(MerchandiseType.builder().typeName("Vinyl Record_" + System.currentTimeMillis()).build()));

        limitedMerch = merchandiseRepository.save(Merchandise.builder()
                .title("Limited Edition Vinyl")
                .seller(sellerUser)
                .category(cat)
                .merchandiseType(type)
                .price(new BigDecimal("50.00"))
                .stockQuantity(1) // Only 1 in stock!
                .build());

        order1 = orderRepository.save(Order.builder()
                .buyer(buyerUser1)
                .status("PENDING")
                .totalCents(5000L)
                .currency("usd")
                .stripePaymentIntentId("pi_stock_conc_1_" + UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order1)
                .itemType("MERCHANDISE")
                .merchandise(limitedMerch)
                .seller(sellerUser)
                .unitPriceCents(5000L)
                .quantity(1)
                .build());

        order2 = orderRepository.save(Order.builder()
                .buyer(buyerUser2)
                .status("PENDING")
                .totalCents(5000L)
                .currency("usd")
                .stripePaymentIntentId("pi_stock_conc_2_" + UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order2)
                .itemType("MERCHANDISE")
                .merchandise(limitedMerch)
                .seller(sellerUser)
                .unitPriceCents(5000L)
                .quantity(1)
                .build());
    }

    @Test
    @DisplayName("Concurrent payments for single stock item results in exactly one PAID and one NEEDS_REFUND")
    void testConcurrentPaymentsForLimitedStock() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Integer> task1 = () -> {
            readyLatch.countDown();
            startLatch.await();
            String payload1 = buildPayload("evt_conc_1_" + UUID.randomUUID(), order1.getStripePaymentIntentId(), 5000);
            String sig1 = StripeTestSigner.computeHeader(payload1, stripeProperties.getWebhookSecret());
            return mockMvc.perform(post("/api/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", sig1)
                            .content(payload1))
                    .andReturn().getResponse().getStatus();
        };

        Callable<Integer> task2 = () -> {
            readyLatch.countDown();
            startLatch.await();
            String payload2 = buildPayload("evt_conc_2_" + UUID.randomUUID(), order2.getStripePaymentIntentId(), 5000);
            String sig2 = StripeTestSigner.computeHeader(payload2, stripeProperties.getWebhookSecret());
            return mockMvc.perform(post("/api/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", sig2)
                            .content(payload2))
                    .andReturn().getResponse().getStatus();
        };

        Future<Integer> f1 = executor.submit(task1);
        Future<Integer> f2 = executor.submit(task2);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        int status1 = f1.get(10, TimeUnit.SECONDS);
        int status2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(status1).isEqualTo(200);
        assertThat(status2).isEqualTo(200);

        Order o1 = orderRepository.findById(order1.getId()).orElseThrow();
        Order o2 = orderRepository.findById(order2.getId()).orElseThrow();

        List<String> statuses = List.of(o1.getStatus(), o2.getStatus());
        assertThat(statuses).containsExactlyInAnyOrder("PAID", "NEEDS_REFUND");

        Merchandise refreshedMerch = merchandiseRepository.findById(limitedMerch.getId()).orElseThrow();
        assertThat(refreshedMerch.getStockQuantity()).isEqualTo(0);
    }

    private String buildPayload(String eventId, String intentId, long amount) {
        return """
                {
                  "id": "%s",
                  "object": "event",
                  "api_version": "2024-06-20",
                  "type": "payment_intent.succeeded",
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "payment_intent",
                      "amount": %d,
                      "amount_received": %d,
                      "currency": "usd",
                      "status": "succeeded"
                    }
                  }
                }
                """.formatted(eventId, intentId, amount, amount);
    }
}
