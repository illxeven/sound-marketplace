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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class PaginationIT {

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
                        .email("page_artist@soundmarketplace.com")
                        .passwordHash("hashedPass123")
                        .build()));

        Genre genre = genreRepository.findByNameIgnoreCase("PageGenre").orElseGet(() ->
                genreRepository.save(Genre.builder().name("PageGenre").build()));

        List<Track> batch = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusHours(30);
        for (int i = 1; i <= 25; i++) {
            batch.add(Track.builder()
                    .title("Track Number " + String.format("%02d", i))
                    .artist(artist)
                    .artistName("Page Artist")
                    .genre(genre)
                    .price(BigDecimal.valueOf(i))
                    .filePath("/uploads/tracks/p_" + i + ".mp3")
                    .createdAt(baseTime.plusMinutes(i))
                    .build());
        }
        trackRepository.saveAll(batch);
    }

    @Test
    @DisplayName("NTF-17: Default page size is 20 and total count matches 25")
    void testDefaultPagination() throws Exception {
        mockMvc.perform(get("/api/tracks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(25))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(20));
    }

    @Test
    @DisplayName("NTF-17: Page size is strictly capped at 100")
    void testPageSizeCappedAt100() throws Exception {
        mockMvc.perform(get("/api/tracks?size=250"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    @DisplayName("NTF-17: Sorting parameter functions as requested (e.g. createdAt,desc)")
    void testSorting() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tracks?page=0&size=5&sort=createdAt,desc"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode items = root.path("data").path("items");
        assertThat(items.size()).isEqualTo(5);
        // The last added track (i=25) should appear first when sorted createdAt,desc
        assertThat(items.get(0).path("title").asText()).isEqualTo("Track Number 25");
    }
}
