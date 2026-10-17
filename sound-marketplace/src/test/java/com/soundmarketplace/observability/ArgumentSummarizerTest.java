package com.soundmarketplace.observability;

import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArgumentSummarizerTest {

    @Test
    @DisplayName("Redaction: sensitive names are replaced with ***")
    void testSensitiveNamesRedaction() {
        assertThat(ArgumentSummarizer.summarize("password", "SuperSecret123")).isEqualTo("***");
        assertThat(ArgumentSummarizer.summarize("token", "abc123token")).isEqualTo("***");
        assertThat(ArgumentSummarizer.summarize("secret", "my-api-secret")).isEqualTo("***");
        assertThat(ArgumentSummarizer.summarize("clientSecret", "pi_123_secret_456")).isEqualTo("***");
        assertThat(ArgumentSummarizer.summarize("ephemeralKey", "ek_123456")).isEqualTo("***");
        assertThat(ArgumentSummarizer.summarize("authorization", "Bearer eyJhbGciOi...")).isEqualTo("***");
        assertThat(ArgumentSummarizer.summarize("cardNumber", "4242424242424242")).isEqualTo("***");
        assertThat(ArgumentSummarizer.summarize("cvc", "123")).isEqualTo("***");
        assertThat(ArgumentSummarizer.summarize("email", "artist@example.com")).isEqualTo("***");
        assertThat(ArgumentSummarizer.summarize("userEmail", "test@soundmarketplace.com")).isEqualTo("***");
    }

    @Test
    @DisplayName("Truncation: strings longer than 64 characters are truncated")
    void testStringTruncation() {
        String longString = "This is a very long string that definitely exceeds sixty-four characters in total length.";
        String summarized = ArgumentSummarizer.summarize("description", longString);

        assertThat(summarized.length()).isLessThanOrEqualTo(64);
        assertThat(summarized).endsWith("...");
    }

    @Test
    @DisplayName("Byte array summarization: formatted as <bytes:N>")
    void testByteArraySummarization() {
        byte[] bytes = new byte[1024];
        assertThat(ArgumentSummarizer.summarize("audioBytes", bytes)).isEqualTo("<bytes:1024>");
    }

    @Test
    @DisplayName("MultipartFile summarization: formatted as <file:name,size,contentType>")
    void testMultipartFileSummarization() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "beat.mp3", "audio/mpeg", new byte[2048]
        );
        assertThat(ArgumentSummarizer.summarize("file", file)).isEqualTo("<file:beat.mp3,2048,audio/mpeg>");
    }

    @Test
    @DisplayName("Collections: formatted as <list:N>")
    void testCollectionSummarization() {
        List<String> list = List.of("item1", "item2", "item3");
        assertThat(ArgumentSummarizer.summarize("items", list)).isEqualTo("<list:3>");
    }

    @Test
    @DisplayName("Entities: formatted as Type#id")
    void testEntitySummarization() {
        UserAccount user = UserAccount.builder().id(42).email("artist@soundmarketplace.com").build();
        assertThat(ArgumentSummarizer.summarize("user", user)).isEqualTo("UserAccount#42");
    }

    @Test
    @DisplayName("Primitives: numbers and booleans are printed as-is")
    void testPrimitivesSummarization() {
        assertThat(ArgumentSummarizer.summarize("userId", 101)).isEqualTo("101");
        assertThat(ArgumentSummarizer.summarize("isActive", true)).isEqualTo("true");
        assertThat(ArgumentSummarizer.summarize("amount", 29.99)).isEqualTo("29.99");
    }
}
