package com.soundmarketplace.media;

import com.soundmarketplace.security.JwtTestTokens;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class BlobExportIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTestTokens jwtTestTokens;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserAccount admin;
    private Track trackWithBlob;
    private byte[] sampleImageBytes;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE;");
        jdbcTemplate.execute("TRUNCATE TABLE entitlements;");
        jdbcTemplate.execute("TRUNCATE TABLE playback_history;");
        jdbcTemplate.execute("TRUNCATE TABLE track_likes;");
        jdbcTemplate.execute("TRUNCATE TABLE track_comments;");
        jdbcTemplate.execute("TRUNCATE TABLE playlist_tracks;");
        jdbcTemplate.execute("TRUNCATE TABLE tracks;");
        jdbcTemplate.execute("TRUNCATE TABLE albums;");
        jdbcTemplate.execute("TRUNCATE TABLE user_profile;");
        jdbcTemplate.execute("TRUNCATE TABLE merchandise;");
        jdbcTemplate.execute("TRUNCATE TABLE user_account;");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE;");

        admin = new UserAccount();
        admin.setEmail("admin@test.com");
        admin.setPasswordHash("password");
        admin.setIsEmailVerified(true);
        admin.setIsActive(true);
        admin = userAccountRepository.save(admin);

        // Valid PNG header bytes
        sampleImageBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4, 5};

        trackWithBlob = new Track();
        trackWithBlob.setTitle("Blob Track");
        trackWithBlob.setArtist(admin);
        trackWithBlob.setPrice(BigDecimal.ZERO);
        trackWithBlob.setStatus("READY");
        trackWithBlob.setFilePath("/tracks/blob_track.mp3");
        trackWithBlob.setCoverImageBlob(sampleImageBytes);
        trackWithBlob = trackRepository.save(trackWithBlob);
    }

    @Test
    @DisplayName("Export blobs moves DB blobs to storage, nulls blob columns, and retains GET cover bytes")
    void exportBlobsMigratesToStorageAndClearsBlob() throws Exception {
        String adminToken = jwtTestTokens.admin(admin.getId());

        // 1. Call export blobs endpoint
        mockMvc.perform(post("/api/admin/media/export-blobs")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 2. Refresh track from database
        Track updated = trackRepository.findById(trackWithBlob.getId()).orElseThrow();
        assertThat(updated.getCoverImageBlob()).isNull();
        assertThat(updated.getCoverKey()).isNotNull();
        assertThat(storageService.exists(updated.getCoverKey())).isTrue();

        // 3. GET /api/tracks/{id}/cover returns identical bytes
        mockMvc.perform(get("/api/tracks/" + updated.getId() + "/cover"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(sampleImageBytes));
    }
}
