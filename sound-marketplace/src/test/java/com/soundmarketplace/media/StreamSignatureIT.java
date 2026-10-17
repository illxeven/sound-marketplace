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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class StreamSignatureIT {

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
        artist.setEmail("streamartist@test.com");
        artist.setPasswordHash("password");
        artist.setIsEmailVerified(true);
        artist.setIsActive(true);
        artist = userAccountRepository.save(artist);

        testTrack = new Track();
        testTrack.setTitle("Stream Test Track");
        testTrack.setArtist(artist);
        testTrack.setPrice(BigDecimal.ZERO);
        testTrack.setStatus("READY");
        testTrack.setFilePath("/tracks/stream_test/stream.m4a");
        testTrack.setStreamKey("tracks/stream_test/stream.m4a");
        testTrack.setPreviewKey("tracks/stream_test/preview.m4a");
        testTrack = trackRepository.save(testTrack);

        byte[] audioData = "dummy-audio-content-bytes-1234567890".getBytes(StandardCharsets.UTF_8);
        storageService.store("tracks/stream_test/stream.m4a", audioData, "audio/mp4");
        storageService.store("tracks/stream_test/preview.m4a", audioData, "audio/mp4");
    }

    @Test
    @DisplayName("Valid signed URL returns 200 with audio bytes")
    void validSignatureReturns200() throws Exception {
        long exp = Instant.now().getEpochSecond() + 300;
        String sig = playbackGateService.sign(testTrack.getId(), "full", exp);

        mockMvc.perform(get("/api/stream/" + testTrack.getId())
                        .param("v", "full")
                        .param("exp", String.valueOf(exp))
                        .param("sig", sig))
                .andExpect(status().isOk())
                .andExpect(content().bytes("dummy-audio-content-bytes-1234567890".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Expired exp returns 403 Forbidden")
    void expiredSignatureReturns403() throws Exception {
        long exp = Instant.now().getEpochSecond() - 60; // expired 1 minute ago
        String sig = playbackGateService.sign(testTrack.getId(), "full", exp);

        mockMvc.perform(get("/api/stream/" + testTrack.getId())
                        .param("v", "full")
                        .param("exp", String.valueOf(exp))
                        .param("sig", sig))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tampered sig returns 403 Forbidden")
    void tamperedSignatureReturns403() throws Exception {
        long exp = Instant.now().getEpochSecond() + 300;

        mockMvc.perform(get("/api/stream/" + testTrack.getId())
                        .param("v", "full")
                        .param("exp", String.valueOf(exp))
                        .param("sig", "invalid-tampered-signature-base64"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Wrong trackId or stream version returns 403 Forbidden")
    void wrongTrackIdOrVersionReturns403() throws Exception {
        long exp = Instant.now().getEpochSecond() + 300;
        String sigForFull = playbackGateService.sign(testTrack.getId(), "full", exp);

        // Tamper version: use 'preview' with full's signature
        mockMvc.perform(get("/api/stream/" + testTrack.getId())
                        .param("v", "preview")
                        .param("exp", String.valueOf(exp))
                        .param("sig", sigForFull))
                .andExpect(status().isForbidden());

        // Tamper trackId: use 9999 with testTrack's signature
        mockMvc.perform(get("/api/stream/9999")
                        .param("v", "full")
                        .param("exp", String.valueOf(exp))
                        .param("sig", sigForFull))
                .andExpect(status().isForbidden());
    }
}
