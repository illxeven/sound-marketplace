package com.soundmarketplace.media;

import com.soundmarketplace.service.PlaybackGateService;
import com.soundmarketplace.storage.StorageService;
import com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class RangeRequestIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlaybackGateService playbackGateService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Track testTrack;
    private byte[] audioBytes;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE;");
        jdbcTemplate.execute("TRUNCATE TABLE entitlements;");
        jdbcTemplate.execute("TRUNCATE TABLE playback_history;");
        jdbcTemplate.execute("TRUNCATE TABLE track_likes;");
        jdbcTemplate.execute("TRUNCATE TABLE track_comments;");
        jdbcTemplate.execute("TRUNCATE TABLE playlist_tracks;");
        jdbcTemplate.execute("TRUNCATE TABLE tracks;");
        jdbcTemplate.execute("TRUNCATE TABLE user_account;");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE;");

        UserAccount artist = new UserAccount();
        artist.setEmail("rangeartist@test.com");
        artist.setPasswordHash("password");
        artist.setIsEmailVerified(true);
        artist.setIsActive(true);
        artist = userAccountRepository.save(artist);

        testTrack = new Track();
        testTrack.setTitle("Range Test Track");
        testTrack.setArtist(artist);
        testTrack.setPrice(BigDecimal.ZERO);
        testTrack.setStatus("READY");
        testTrack.setFilePath("/tracks/range_test/stream.m4a");
        testTrack.setStreamKey("tracks/range_test/stream.m4a");
        testTrack = trackRepository.save(testTrack);

        // 4096 bytes of test audio
        audioBytes = new byte[4096];
        for (int i = 0; i < audioBytes.length; i++) {
            audioBytes[i] = (byte) (i % 128);
        }
        storageService.store("tracks/range_test/stream.m4a", audioBytes, "audio/mp4");
    }

    @Test
    @DisplayName("Range: bytes=0-1023 returns 206 Partial Content, Content-Range, and 1024 bytes")
    void rangeRequestReturnsPartialContent() throws Exception {
        long exp = Instant.now().getEpochSecond() + 300;
        String sig = playbackGateService.sign(testTrack.getId(), "full", exp);

        MvcResult result = mockMvc.perform(get("/api/stream/" + testTrack.getId())
                        .param("v", "full")
                        .param("exp", String.valueOf(exp))
                        .param("sig", sig)
                        .header(HttpHeaders.RANGE, "bytes=0-1023"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-1023/4096"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, max-age=300"))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).hasSize(1024);
        for (int i = 0; i < 1024; i++) {
            assertThat(content[i]).isEqualTo(audioBytes[i]);
        }
    }

    @Test
    @DisplayName("Standard GET without Range returns 200 OK and Accept-Ranges: bytes")
    void fullRequestReturns200WithAcceptRanges() throws Exception {
        long exp = Instant.now().getEpochSecond() + 300;
        String sig = playbackGateService.sign(testTrack.getId(), "full", exp);

        mockMvc.perform(get("/api/stream/" + testTrack.getId())
                        .param("v", "full")
                        .param("exp", String.valueOf(exp))
                        .param("sig", sig))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, max-age=300"));
    }
}
