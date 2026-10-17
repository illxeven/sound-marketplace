package com.soundmarketplace.performance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication;
import com.soundmarketplace.sound_marketplace.domain.Genre;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.GenreRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class TrackListPerformanceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            jdbcTemplate.execute("DELETE FROM entitlements");
            jdbcTemplate.execute("DELETE FROM playback_history");
            jdbcTemplate.execute("DELETE FROM track_likes");
            jdbcTemplate.execute("DELETE FROM track_comments");
            jdbcTemplate.execute("DELETE FROM playlist_tracks");
            jdbcTemplate.execute("DELETE FROM cart_items");
            jdbcTemplate.execute("DELETE FROM order_items");
            jdbcTemplate.execute("DELETE FROM user_queue_items");
            jdbcTemplate.execute("DELETE FROM tracks");
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        UserAccount artist = userAccountRepository.findAll().stream().findFirst().orElseGet(() ->
                userAccountRepository.save(UserAccount.builder()
                        .email("perf_artist@soundmarketplace.com")
                        .passwordHash("hashedPass123")
                        .build()));

        Genre genre = genreRepository.findByNameIgnoreCase("PerfGenre").orElseGet(() ->
                genreRepository.save(Genre.builder().name("PerfGenre").build()));

        // Seed tracks with 200 KB dummy covers
        byte[] heavyCover = new byte[200 * 1024];
        Arrays.fill(heavyCover, (byte) 0x42);

        List<Track> batch = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            batch.add(Track.builder()
                    .title("Heavy Track " + i)
                    .artist(artist)
                    .artistName("Perf Artist")
                    .genre(genre)
                    .price(BigDecimal.valueOf(1.99))
                    .coverImageBlob(heavyCover)
                    .videoBlob(heavyCover)
                    .filePath("/uploads/tracks/track_" + i + ".mp3")
                    .build());
        }
        trackRepository.saveAll(batch);
    }

    @Test
    @DisplayName("NTF-14/16/17: GET /api/tracks?size=20 gives response < 50 KB, sqlCount <= 3, and no blob/path fields")
    void testTrackListPerformance() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tracks?page=0&size=20"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] responseBytes = result.getResponse().getContentAsByteArray();
        assertThat(responseBytes.length)
                .withFailMessage("Response body size %d bytes exceeded 50 KB limit", responseBytes.length)
                .isLessThan(50 * 1024);

        String sqlCountHeader = result.getResponse().getHeader("X-Sql-Count");
        if (sqlCountHeader != null) {
            int sqlCount = Integer.parseInt(sqlCountHeader);
            assertThat(sqlCount)
                    .withFailMessage("Expected sqlCount <= 3 for paged query projection, but got %d", sqlCount)
                    .isLessThanOrEqualTo(3);
        }

        String json = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        JsonNode items = root.path("data").path("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isEqualTo(20);

        for (JsonNode item : items) {
            assertThat(item.has("coverImageBlob")).isFalse();
            assertThat(item.has("videoBlob")).isFalse();
            assertThat(item.has("filePath")).isFalse();
            assertThat(item.has("videoFilePath")).isFalse();
            assertThat(item.has("coverUrl")).isTrue();
        }
    }
}
