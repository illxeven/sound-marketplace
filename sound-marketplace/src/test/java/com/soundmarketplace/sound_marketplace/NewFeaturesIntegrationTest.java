package com.soundmarketplace.sound_marketplace;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.service.EmailService;
import com.soundmarketplace.service.LegalComplianceService;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.dto.*;
import com.soundmarketplace.sound_marketplace.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NewFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.soundmarketplace.security.JwtTestTokens jwtTestTokens;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    @Autowired
    private LegalDocumentRepository legalDocumentRepository;

    @Autowired
    private UserAgreementRepository userAgreementRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private MerchandiseRepository merchandiseRepository;

    @Autowired
    private MerchandiseTypeRepository merchandiseTypeRepository;

    private UserAccount testArtist;
    private UserAccount testConsumer;

    @BeforeEach
    void setUp() {
        trackRepository.deleteAll();
        merchandiseRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userAgreementRepository.deleteAll();
        legalDocumentRepository.deleteAll();
        emailLogRepository.deleteAll();

        Role artistRole = roleRepository.findByRoleNameIgnoreCase("music_artist")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("music_artist").build()));
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));

        userAccountRepository.findByEmail("artist_feature@soundmarketplace.com").ifPresent(u -> {
            userProfileRepository.findByUserAccountId(u.getId()).ifPresent(userProfileRepository::delete);
            userAccountRepository.delete(u);
        });

        userAccountRepository.findByEmail("consumer_feature@soundmarketplace.com").ifPresent(u -> {
            userProfileRepository.findByUserAccountId(u.getId()).ifPresent(userProfileRepository::delete);
            userAccountRepository.delete(u);
        });

        testArtist = userAccountRepository.save(UserAccount.builder()
                .email("artist_feature@soundmarketplace.com")
                .passwordHash("hashedPass123")
                .isActive(true)
                .roles(Set.of(artistRole))
                .build());

        userProfileRepository.save(UserProfile.builder()
                .userAccount(testArtist)
                .displayName("Star Artist")
                .bio("Original sound maker")
                .avatarUrl("https://example.com/avatar.jpg")
                .country("US")
                .build());

        testConsumer = userAccountRepository.save(UserAccount.builder()
                .email("consumer_feature@soundmarketplace.com")
                .passwordHash("hashedPass123")
                .isActive(true)
                .roles(Set.of(consumerRole))
                .build());

        userProfileRepository.save(UserProfile.builder()
                .userAccount(testConsumer)
                .displayName("Music Listener")
                .country("UK")
                .build());
    }

    @Test
    @DisplayName("Password & Username Recovery: Flow generates secure token and resets password")
    void testPasswordAndUsernameRecoveryFlow() throws Exception {
        // 1. Request Password Reset
        ForgotPasswordRequest forgotReq = ForgotPasswordRequest.builder()
                .email("artist_feature@soundmarketplace.com")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify token saved and email logged
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
        assertFalse(tokens.isEmpty());
        String token = tokens.get(0).getToken();

        List<EmailLog> emails = emailLogRepository.findByRecipientEmailOrderBySentAtDesc("artist_feature@soundmarketplace.com");
        assertFalse(emails.isEmpty());
        assertTrue(List.of("SENT", "FAILED").contains(emails.get(0).getStatus()));
        assertTrue(emails.get(0).getBody().contains(token));

        // 2. Reset Password with token
        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token(token)
                .newPassword("brandNewPassword99")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        PasswordResetToken updatedToken = passwordResetTokenRepository.findByToken(token).orElseThrow();
        assertTrue(updatedToken.getUsed());

        // 3. Recover Username
        mockMvc.perform(post("/api/auth/recover-username?email=artist_feature@soundmarketplace.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("Edit Profile: Allows updating bio, avatarUrl, displayName, and country")
    void testFullProfileEditing() throws Exception {
        UpdateProfileRequest updateReq = UpdateProfileRequest.builder()
                .displayName("Star Artist Updated")
                .bio("Producer and beatmaker extraordinaire")
                .avatarUrl("https://example.com/new-avatar.png")
                .country("Germany")
                .build();

        mockMvc.perform(put("/api/users/" + testArtist.getId() + "/profile")
                .header("Authorization", jwtTestTokens.artist(testArtist.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.displayName", is("Star Artist Updated")))
                .andExpect(jsonPath("$.data.bio", is("Producer and beatmaker extraordinaire")))
                .andExpect(jsonPath("$.data.avatarUrl", is("https://example.com/new-avatar.png")))
                .andExpect(jsonPath("$.data.country", is("Germany")));
    }

    @Test
    @DisplayName("Public Profile: SoundCloud-style view shows public tracks, albums, and followers")
    void testSoundCloudStylePublicProfile() throws Exception {
        trackRepository.save(Track.builder()
                .artist(testArtist)
                .title("Neon City Beats")
                .filePath("/uploads/tracks/neon.mp3")
                .artistName("Star Artist")
                .genre(null)
                .price(BigDecimal.valueOf(1.99))
                .build());

        mockMvc.perform(get("/api/users/" + testArtist.getId() + "/public-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.displayName", is("Star Artist")))
                .andExpect(jsonPath("$.data.tracks", hasSize(1)))
                .andExpect(jsonPath("$.data.tracks[0].title", is("Neon City Beats")));
    }

    @Test
    @DisplayName("Track Upload Permissions: Consumers cannot upload tracks (403 Forbidden)")
    void testConsumerCannotUploadTrack() throws Exception {
        CreateTrackRequest request = CreateTrackRequest.builder()
                .artistId(testConsumer.getId())
                .uploaderUserId(testConsumer.getId())
                .uploadSource("PROFILE_PAGE")
                .title("Unauthorized Track")
                .filePath("s3://test.mp3")
                .build();

        mockMvc.perform(post("/api/tracks")
                .header("Authorization", jwtTestTokens.consumer(testConsumer.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Consumers cannot upload music")));
    }

    @Test
    @DisplayName("Track Upload Location: Reject uploads initiated from discover page")
    void testRejectUploadFromDiscoverPage() throws Exception {
        CreateTrackRequest request = CreateTrackRequest.builder()
                .artistId(testArtist.getId())
                .uploaderUserId(testArtist.getId())
                .uploadSource("DISCOVER_PAGE") // Invalid location!
                .title("Discover Track")
                .filePath("s3://test.mp3")
                .build();

        mockMvc.perform(post("/api/tracks")
                .header("Authorization", jwtTestTokens.artist(testArtist.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("exclusively from the user's own profile page")));
    }

    @Test
    @DisplayName("Multipart File Upload: Supports file picker audio upload for tracks")
    void testMultipartTrackUpload() throws Exception {
        MockMultipartFile audioFile = new MockMultipartFile(
                "audioFile",
                "test-song.mp3",
                "audio/mpeg",
                "audio-bytes-content".getBytes()
        );

        mockMvc.perform(multipart("/api/tracks/upload")
                .file(audioFile)
                .header("Authorization", jwtTestTokens.artist(testArtist.getId()))
                .param("title", "Multipart Synth Wave")
                .param("artistId", testArtist.getId().toString())
                .param("uploaderUserId", testArtist.getId().toString())
                .param("artistName", "Star Artist")
                .param("genre", "Synthwave")
                .param("price", "2.99")
                .param("uploadSource", "PROFILE_PAGE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Multipart Synth Wave")))
                .andExpect(jsonPath("$.data.filePath", containsString(".mp3")));
    }

    @Test
    @DisplayName("Legal & Compliance: Fetches active policies and records user consent with IP")
    void testLegalComplianceAndConsent() throws Exception {
        DocumentType termsType = documentTypeRepository.findByNameIgnoreCase("TERMS_OF_SERVICE")
                .orElseGet(() -> documentTypeRepository.save(DocumentType.builder().name("TERMS_OF_SERVICE").description("Terms of service").build()));

        LegalDocument doc = legalDocumentRepository.save(LegalDocument.builder()
                .version("v1.0")
                .content("Standard terms and conditions...")
                .isActive(true)
                .effectiveDate(LocalDateTime.now())
                .createdBy(1L)
                .documentTypes(Set.of(termsType))
                .build());

        // 1. Fetch active terms
        mockMvc.perform(get("/api/legal/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].version", is("v1.0")));

        // 2. Record consent
        UserConsentRequest consentReq = UserConsentRequest.builder()
                .userId((long) testArtist.getId())
                .documentId(doc.getId())
                .build();

        mockMvc.perform(post("/api/legal/consent")
                .header("Authorization", jwtTestTokens.artist(testArtist.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "192.168.1.100")
                .content(objectMapper.writeValueAsString(consentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.ipAddress", is("192.168.1.100")));
    }

    @Test
    @DisplayName("Marketplace & Filters: Filter items by category (beats, songs, merch)")
    void testMarketplaceCategoryFiltering() throws Exception {
        // Add a beat
        trackRepository.save(Track.builder()
                .title("Trap Beat 140BPM")
                .genre(null)
                .filePath("/uploads/tracks/beat.mp3")
                .price(BigDecimal.valueOf(25.00))
                .build());

        // Add a song
        trackRepository.save(Track.builder()
                .title("Melodic Pop Song")
                .genre(null)
                .filePath("/uploads/tracks/pop.mp3")
                .price(BigDecimal.valueOf(1.29))
                .build());

        // Query Beats category
        mockMvc.perform(get("/api/marketplace/items?category=beats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].itemType", is("BEAT")));

        // Query Songs category
        mockMvc.perform(get("/api/marketplace/items?category=songs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].itemType", is("SONG")));
    }


}
