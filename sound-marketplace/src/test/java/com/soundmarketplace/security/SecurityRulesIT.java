package com.soundmarketplace.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class SecurityRulesIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private JwtTestTokens jwtTestTokens;

    private static final Set<String> PUBLIC_POST_PREFIXES = Set.of(
            "/api/auth/",
            "/api/payments/webhook"
    );

    /** Exact public write routes (prefix matching "/api/users" would wrongly cover every /api/users/** route). */
    private static final Set<String> PUBLIC_POST_EXACT = Set.of("/api/users");

    @Test
    @DisplayName("Route inventory scan: unauthenticated write endpoints return 401")
    void unauthenticatedWritesReturn401() throws Exception {
        var handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        assertThat(handlerMethods).isNotEmpty();

        for (var entry : handlerMethods.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            var patterns = info.getPatternValues();
            var methods = info.getMethodsCondition().getMethods();

            for (String pattern : patterns) {
                for (var method : methods) {
                    if (method.asHttpMethod() == HttpMethod.POST ||
                        method.asHttpMethod() == HttpMethod.PUT ||
                        method.asHttpMethod() == HttpMethod.DELETE) {

                        boolean isPublic = PUBLIC_POST_PREFIXES.stream().anyMatch(pattern::startsWith)
                                || PUBLIC_POST_EXACT.contains(pattern)
                                || pattern.endsWith("/play-event")
                                || pattern.equals("/api/tracks/play-urls");
                        if (!isPublic) {
                            // Resolve simple path variable placeholders if any
                            String testPath = pattern.replaceAll("\\{[^}]+\\}", "1");

                            int status = mockMvc.perform(
                                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(method.asHttpMethod(), testPath)
                                            .contentType(MediaType.APPLICATION_JSON))
                                    .andReturn().getResponse().getStatus();

                            assertThat(status)
                                    .withFailMessage("Expected 401 for unauthenticated %s %s, but got %d", method, testPath, status)
                                    .isEqualTo(401);
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Admin-only endpoints reject consumer token with 403 and accept admin token")
    void adminEndpointsRbac() throws Exception {
        String consumerToken = jwtTestTokens.consumer(999);
        String adminToken = jwtTestTokens.admin(998);

        // GET /api/users (requires ADMIN or SUPER_ADMIN)
        mockMvc.perform(get("/api/users")
                        .header("Authorization", consumerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public GET endpoints allow unauthenticated access")
    void publicGetEndpointsAllowUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/tracks"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/genres"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/legal/active"))
                .andExpect(status().isOk());
    }
}
