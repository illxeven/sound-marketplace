package com.soundmarketplace.performance;

import com.soundmarketplace.security.JwtTestTokens;
import com.soundmarketplace.service.EngagementService;
import com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication;
import com.soundmarketplace.sound_marketplace.domain.Role;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.RoleRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackLikeRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class CountersIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private TrackLikeRepository trackLikeRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTestTokens jwtTestTokens;

    @Autowired
    private EngagementService engagementService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Track testTrack;
    private UserAccount testUser;
    private List<UserAccount> concurrentUsers;

    @BeforeEach
    void setUp() {
        engagementService.clearPlayDedupeCache();
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

        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));

        testUser = userAccountRepository.save(UserAccount.builder()
                .email("counter_user_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("pass")
                .isActive(true)
                .roles(Set.of(consumerRole))
                .build());

        testTrack = trackRepository.save(Track.builder()
                .title("Counter Track")
                .price(BigDecimal.valueOf(1.99))
                .filePath("/uploads/tracks/ct.mp3")
                .likeCount(0)
                .playCount(0L)
                .build());

        concurrentUsers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            concurrentUsers.add(userAccountRepository.save(UserAccount.builder()
                    .email("concurrent_user_" + i + "_" + System.currentTimeMillis() + "@test.com")
                    .passwordHash("pass")
                    .isActive(true)
                    .roles(Set.of(consumerRole))
                    .build()));
        }
    }

    @Test
    @DisplayName("NTF-16: Liking and unliking a track updates atomic like_count in sync with rows")
    void testLikeUnlikeAtomicCounters() throws Exception {
        // 1. Like track
        mockMvc.perform(post("/api/tracks/" + testTrack.getId() + "/like")
                .header("Authorization", jwtTestTokens.consumer(testUser.getId())))
                .andExpect(status().isOk());

        Track afterLike = trackRepository.findById(testTrack.getId()).orElseThrow();
        long actualLikeRows = trackLikeRepository.countByTrackId(testTrack.getId());
        assertThat(afterLike.getLikeCount()).isEqualTo(1);
        assertThat(actualLikeRows).isEqualTo(1L);

        // 2. Duplicate like from same user is a no-op
        mockMvc.perform(post("/api/tracks/" + testTrack.getId() + "/like")
                .header("Authorization", jwtTestTokens.consumer(testUser.getId())))
                .andExpect(status().isOk());

        afterLike = trackRepository.findById(testTrack.getId()).orElseThrow();
        assertThat(afterLike.getLikeCount()).isEqualTo(1);

        // 3. Unlike track
        mockMvc.perform(delete("/api/tracks/" + testTrack.getId() + "/like")
                .header("Authorization", jwtTestTokens.consumer(testUser.getId())))
                .andExpect(status().isOk());

        Track afterUnlike = trackRepository.findById(testTrack.getId()).orElseThrow();
        actualLikeRows = trackLikeRepository.countByTrackId(testTrack.getId());
        assertThat(afterUnlike.getLikeCount()).isEqualTo(0);
        assertThat(actualLikeRows).isEqualTo(0L);
    }

    @Test
    @DisplayName("NTF-16: Play events update atomic play_count and deduplicate within 30 minutes")
    void testPlayEventDeduplication() throws Exception {
        // First play event: should record and increment
        mockMvc.perform(post("/api/tracks/" + testTrack.getId() + "/play-event")
                .header("X-Request-Id", "req-unique-1234")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recorded").value(true))
                .andExpect(jsonPath("$.data.playCount").value(1));

        Track afterFirstPlay = trackRepository.findById(testTrack.getId()).orElseThrow();
        assertThat(afterFirstPlay.getPlayCount()).isEqualTo(1L);

        // Second play event with same client identifier within 30 min: should deduplicate
        mockMvc.perform(post("/api/tracks/" + testTrack.getId() + "/play-event")
                .header("X-Request-Id", "req-unique-1234")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recorded").value(false))
                .andExpect(jsonPath("$.data.playCount").value(1));

        Track afterSecondPlay = trackRepository.findById(testTrack.getId()).orElseThrow();
        assertThat(afterSecondPlay.getPlayCount()).isEqualTo(1L);

        // Third play event with distinct client identifier: should record
        mockMvc.perform(post("/api/tracks/" + testTrack.getId() + "/play-event")
                .header("X-Request-Id", "req-different-5678")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recorded").value(true))
                .andExpect(jsonPath("$.data.playCount").value(2));

        Track afterThirdPlay = trackRepository.findById(testTrack.getId()).orElseThrow();
        assertThat(afterThirdPlay.getPlayCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("NTF-16: Concurrent likes from 10 threads atomically increment like_count to exactly 10")
    void testConcurrentLikes() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    mockMvc.perform(post("/api/tracks/" + testTrack.getId() + "/like")
                            .header("Authorization", jwtTestTokens.consumer(concurrentUsers.get(index).getId())));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();

        Track finalTrack = trackRepository.findById(testTrack.getId()).orElseThrow();
        long actualRows = trackLikeRepository.countByTrackId(testTrack.getId());
        assertThat(finalTrack.getLikeCount()).isEqualTo(10);
        assertThat(actualRows).isEqualTo(10L);
    }
}
