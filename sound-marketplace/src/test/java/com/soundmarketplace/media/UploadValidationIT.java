package com.soundmarketplace.media;

import com.soundmarketplace.security.JwtTestTokens;
import com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication;
import com.soundmarketplace.sound_marketplace.domain.Role;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.RoleRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
@Import(UploadValidationIT.TestMediaConfig.class)
public class UploadValidationIT {

    @TestConfiguration
    static class TestMediaConfig {
        @Bean
        @Primary
        public DurationProbe testDurationProbe() {
            return file -> 180;
        }

        @Bean
        @Primary
        public MediaTranscoder testMediaTranscoder() {
            return new MediaTranscoder() {
                @Override
                public TranscodeResult transcodeAudio(File inputFile, Integer trackId, Integer durationSeconds) {
                    return new TranscodeResult(true, "tracks/" + trackId + "/stream.m4a", "tracks/" + trackId + "/preview.m4a", "tracks/" + trackId + "/waveform.json", null);
                }

                @Override
                public TranscodeResult transcodeVideo(File inputFile, Integer trackId) {
                    return new TranscodeResult(true, "tracks/" + trackId + "/video.mp4", null, null, null);
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTestTokens jwtTestTokens;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserAccount artistUser;
    private UserAccount consumerUser;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE;");
        jdbcTemplate.execute("TRUNCATE TABLE entitlements;");
        jdbcTemplate.execute("TRUNCATE TABLE playback_history;");
        jdbcTemplate.execute("TRUNCATE TABLE track_likes;");
        jdbcTemplate.execute("TRUNCATE TABLE track_comments;");
        jdbcTemplate.execute("TRUNCATE TABLE playlist_tracks;");
        jdbcTemplate.execute("TRUNCATE TABLE tracks;");
        jdbcTemplate.execute("TRUNCATE TABLE user_roles;");
        jdbcTemplate.execute("TRUNCATE TABLE user_account;");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE;");

        Role artistRole = roleRepository.findByRoleNameIgnoreCase("music_artist")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("music_artist").build()));
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));

        artistUser = userAccountRepository.save(UserAccount.builder()
                .email("artist_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hashedpw")
                .roles(Set.of(artistRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());

        consumerUser = userAccountRepository.save(UserAccount.builder()
                .email("consumer_" + System.currentTimeMillis() + "@test.com")
                .passwordHash("hashedpw")
                .roles(Set.of(consumerRole))
                .isEmailVerified(true)
                .isActive(true)
                .build());
    }

    private byte[] createMinimalWav() {
        byte[] header = new byte[44 + 100];
        // "RIFF"
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        int totalDataLen = 36 + 100;
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        // "WAVE"
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        // "fmt "
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0; // 16 for PCM
        header[20] = 1; header[21] = 0; // PCM format = 1
        header[22] = 1; header[23] = 0; // 1 channel
        int sampleRate = 44100;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        int byteRate = sampleRate * 2;
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = 2; header[33] = 0; // block align = 2
        header[34] = 16; header[35] = 0; // bits per sample = 16
        // "data"
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = 100; header[41] = 0; header[42] = 0; header[43] = 0; // data size
        return header;
    }

    @Test
    @DisplayName("File with .mp3 extension containing EXE magic bytes returns 415")
    void exeUploadedAsMp3Returns415() throws Exception {
        String token = jwtTestTokens.artist(artistUser.getId());
        byte[] exeBytes = new byte[]{'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00};

        MockMultipartFile fakeMp3 = new MockMultipartFile(
                "audioFile",
                "malicious.mp3",
                "audio/mpeg",
                exeBytes
        );

        mockMvc.perform(multipart("/api/tracks/upload")
                        .file(fakeMp3)
                        .param("title", "Trojan Track")
                        .param("artistId", artistUser.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("File exceeding max size returns 413 Payload Too Large")
    void oversizeFileReturns413() throws Exception {
        String token = jwtTestTokens.artist(artistUser.getId());

        MockMultipartFile largeFile = new MockMultipartFile(
                "audioFile",
                "large.wav",
                "audio/wav",
                createMinimalWav()
        ) {
            @Override
            public long getSize() {
                return 200_000_000L; // 200MB > 100MB limit
            }
        };

        mockMvc.perform(multipart("/api/tracks/upload")
                        .file(largeFile)
                        .param("title", "Too Big Track")
                        .param("artistId", artistUser.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    @DisplayName("Consumer role uploading returns 403 Forbidden")
    void consumerRoleUploadReturns403() throws Exception {
        String token = jwtTestTokens.consumer(consumerUser.getId());

        MockMultipartFile validWav = new MockMultipartFile(
                "audioFile",
                "consumer_song.wav",
                "audio/wav",
                createMinimalWav()
        );

        mockMvc.perform(multipart("/api/tracks/upload")
                        .file(validWav)
                        .param("title", "Consumer Upload Attempt")
                        .param("artistId", consumerUser.getId().toString())
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Duration probe runs on upload; duration stored matches probe output (180), not client claim (999)")
    void durationStoredMatchesProbeNotClient() throws Exception {
        String token = jwtTestTokens.artist(artistUser.getId());

        MockMultipartFile validWav = new MockMultipartFile(
                "audioFile",
                "artist_song.wav",
                "audio/wav",
                createMinimalWav()
        );

        MvcResult result = mockMvc.perform(multipart("/api/tracks/upload")
                        .file(validWav)
                        .param("title", "Probed Track")
                        .param("artistId", artistUser.getId().toString())
                        .param("durationSeconds", "999") // Client claims 999
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.durationSeconds").value(180))
                .andReturn();

        // Also verify persisted in database
        Track savedTrack = trackRepository.findAll().stream()
                .filter(t -> "Probed Track".equals(t.getTitle()))
                .findFirst()
                .orElseThrow();

        assertThat(savedTrack.getDurationSeconds()).isEqualTo(180);
        assertThat(savedTrack.getStatus()).isEqualTo("READY");
        assertThat(savedTrack.getStreamKey()).isNotNull();
        assertThat(savedTrack.getPreviewKey()).isNotNull();
    }
}
