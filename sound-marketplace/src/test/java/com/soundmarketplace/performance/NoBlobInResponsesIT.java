package com.soundmarketplace.performance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class NoBlobInResponsesIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("NTF-14: No GET response returns JSON fields containing large base64 or raw byte strings (> 1000 chars)")
    void testNoBlobInResponses() throws Exception {
        List<String> routes = List.of(
                "/api/tracks?page=0&size=10",
                "/api/albums?page=0&size=10",
                "/api/merchandise?page=0&size=10",
                "/api/marketplace/items",
                "/api/genres"
        );

        for (String route : routes) {
            MvcResult result = mockMvc.perform(get(route)).andReturn();
            if (result.getResponse().getStatus() == 200 &&
                result.getResponse().getContentType() != null &&
                result.getResponse().getContentType().contains("application/json")) {

                String json = result.getResponse().getContentAsString();
                JsonNode root = objectMapper.readTree(json);
                assertNoLongStrings(route, root);
            }
        }
    }

    private void assertNoLongStrings(String route, JsonNode node) {
        if (node.isTextual()) {
            assertThat(node.textValue().length())
                    .withFailMessage("Route %s returned a field with string length %d > 1000 characters: %s",
                            route, node.textValue().length(), node.textValue().substring(0, Math.min(50, node.textValue().length())) + "...")
                    .isLessThan(1000);
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                assertNoLongStrings(route, child);
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                assertThat(fieldName.toLowerCase())
                        .withFailMessage("Route %s returned prohibited blob field name: %s", route, fieldName)
                        .doesNotContain("blob");
                assertNoLongStrings(route, entry.getValue());
            });
        }
    }
}
