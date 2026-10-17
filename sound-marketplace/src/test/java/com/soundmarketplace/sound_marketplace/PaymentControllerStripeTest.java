package com.soundmarketplace.sound_marketplace;

import com.soundmarketplace.config.StripeProperties;
import com.soundmarketplace.service.PaymentService;
import com.soundmarketplace.sound_marketplace.dto.CreatePaymentIntentRequest;
import com.soundmarketplace.sound_marketplace.dto.PaymentIntentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerStripeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.soundmarketplace.security.JwtTestTokens jwtTestTokens;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private StripeProperties stripeProperties;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Verify Stripe Properties and Configuration are loaded")
    void testStripePropertiesLoaded() {
        assertThat(stripeProperties).isNotNull();
        assertThat(stripeProperties.getSecretKey()).isNotNull();
        assertThat(stripeProperties.getPublishableKey()).isNotNull();
        assertThat(stripeProperties.getDefaultCurrency()).isEqualToIgnoringCase("usd");
    }

    @Test
    @DisplayName("Verify PaymentService creates PaymentIntent with clientSecret")
    void testPaymentServiceCreateIntent() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_CONSUMER"))));

        CreatePaymentIntentRequest request = CreatePaymentIntentRequest.builder()
                .amount(new BigDecimal("49.99"))
                .currency("usd")
                .itemDetails("Deluxe Sound Kit Vol 1")
                .itemType("sound_kit")
                .itemId(101)
                .buyerId(1)
                .metadata(Map.of("license", "commercial"))
                .build();

        PaymentIntentResponse response = paymentService.createPaymentIntent(request);

        assertThat(response).isNotNull();
        assertThat(response.getClientSecret()).isNotBlank();
        assertThat(response.getPaymentIntentId()).isNotBlank();
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getStatus()).isNotBlank();
    }

    @Test
    @DisplayName("Verify PaymentService throws exception when amount is null")
    void testPaymentServiceNullAmountThrowsException() {
        CreatePaymentIntentRequest request = CreatePaymentIntentRequest.builder()
                .amount(null)
                .itemDetails("Invalid Request")
                .build();

        assertThatThrownBy(() -> paymentService.createPaymentIntent(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount cannot be null");
    }

    @Test
    @DisplayName("POST /api/payments/create-intent with JSON body returns clientSecret")
    void testCreatePaymentIntentEndpointWithJsonBody() throws Exception {
        String jsonPayload = """
                {
                    "amount": 25.50,
                    "currency": "usd",
                    "itemDetails": "Trap Drums & 808s",
                    "itemType": "sound_kit",
                    "itemId": 42,
                    "buyerId": 7,
                    "metadata": {
                        "category": "drum_kit"
                    }
                }
                """;

        mockMvc.perform(post("/api/payments/create-intent")
                        .header("Authorization", jwtTestTokens.consumer(7))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientSecret").isNotEmpty())
                .andExpect(jsonPath("$.data.paymentIntentId").isNotEmpty())
                .andExpect(jsonPath("$.data.amount").value(25.50))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.status").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/payments/create-intent with query params fallback returns clientSecret")
    void testCreatePaymentIntentEndpointWithQueryParams() throws Exception {
        mockMvc.perform(post("/api/payments/create-intent")
                        .header("Authorization", jwtTestTokens.consumer(1))
                        .param("amount", "19.99")
                        .param("currency", "USD")
                        .param("itemDetails", "Urban Vinyl Hoodie")
                        .param("description", "Merchandise Purchase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientSecret").isNotEmpty())
                .andExpect(jsonPath("$.data.paymentIntentId").isNotEmpty())
                .andExpect(jsonPath("$.data.amount").value(19.99))
                .andExpect(jsonPath("$.data.currency").value("USD"));
    }

    @Test
    @DisplayName("Create and Confirm PaymentIntent transitions status to succeeded and creates transaction")
    void testCreateAndConfirmPaymentIntent() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_CONSUMER"))));

        CreatePaymentIntentRequest request = CreatePaymentIntentRequest.builder()
                .amount(new BigDecimal("10.00"))
                .currency("usd")
                .itemDetails("Test Track Purchase")
                .confirm(true)
                .paymentMethodId("pm_card_visa")
                .build();

        PaymentIntentResponse response = paymentService.createPaymentIntent(request);
        assertThat(response).isNotNull();
        assertThat(response.getPaymentIntentId()).isNotBlank();
        assertThat(response.getStatus()).isEqualTo("succeeded");
    }
}
