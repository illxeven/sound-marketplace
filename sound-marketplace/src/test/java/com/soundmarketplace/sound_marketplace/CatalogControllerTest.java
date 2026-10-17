package com.soundmarketplace.sound_marketplace;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.Album;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.CreateAlbumRequest;
import com.soundmarketplace.sound_marketplace.dto.CreateTrackRequest;
import com.soundmarketplace.sound_marketplace.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.soundmarketplace.security.JwtTestTokens jwtTestTokens;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TrackLikeRepository trackLikeRepository;

    @Autowired
    private TrackCommentRepository trackCommentRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private RoleRepository roleRepository;

    private UserAccount testArtist;

    @BeforeEach
    void setUp() {
        trackCommentRepository.deleteAll();
        trackLikeRepository.deleteAll();
        playlistRepository.deleteAll();
        trackRepository.deleteAll();
        albumRepository.deleteAll();

        com.soundmarketplace.sound_marketplace.domain.Role artistRole = roleRepository.findByRoleNameIgnoreCase("music_artist")
                .orElseGet(() -> roleRepository.save(com.soundmarketplace.sound_marketplace.domain.Role.builder().roleName("music_artist").build()));

        testArtist = userAccountRepository.findByEmail("artist_cat@soundmarketplace.com")
                .orElseGet(() -> userAccountRepository.save(UserAccount.builder()
                        .email("artist_cat@soundmarketplace.com")
                        .passwordHash("password")
                        .isActive(true)
                        .roles(java.util.Set.of(artistRole))
                        .build()));
        if (testArtist.getRoles() == null || testArtist.getRoles().isEmpty()) {
            testArtist.setRoles(java.util.Set.of(artistRole));
            testArtist = userAccountRepository.save(testArtist);
        }
    }

    @Test
    @DisplayName("POST & GET /api/albums creates and retrieves an album")
    void testCreateAndGetAlbum() throws Exception {
        CreateAlbumRequest request = CreateAlbumRequest.builder()
                .artistId(testArtist.getId())
                .title("Dreamwave Vol. 1")
                .coverArtUrl("https://assets.soundmarketplace.com/covers/dreamwave.jpg")
                .build();

        mockMvc.perform(post("/api/albums")
                .header("Authorization", jwtTestTokens.artist(testArtist.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Dreamwave Vol. 1")));

        mockMvc.perform(get("/api/albums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("POST & GET /api/tracks creates, lists, filters, and engages with tracks")
    void testCreateAndInteractWithTrack() throws Exception {
        Album album = albumRepository.save(Album.builder()
                .artist(testArtist)
                .title("Synth Night")
                .build());

        CreateTrackRequest request = CreateTrackRequest.builder()
                .artistId(testArtist.getId())
                .albumId(album.getId())
                .title("Midnight Run")
                .filePath("s3://sounds/midnight_run.wav")
                .genre("Synthwave")
                .price(BigDecimal.valueOf(19.99))
                .build();

        String responseJson = mockMvc.perform(post("/api/tracks")
                .header("Authorization", jwtTestTokens.artist(testArtist.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title", is("Midnight Run")))
                .andExpect(jsonPath("$.data.genre", is("Synthwave")))
                .andReturn().getResponse().getContentAsString();

        Integer trackId = objectMapper.readTree(responseJson).get("data").get("id").asInt();

        // Get by genre
        mockMvc.perform(get("/api/tracks/genre/Synthwave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].title", is("Midnight Run")));

        // Like track
        mockMvc.perform(post("/api/tracks/" + trackId + "/like")
                .header("Authorization", jwtTestTokens.consumer(testArtist.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Track liked")));

        // Like count
        mockMvc.perform(get("/api/tracks/" + trackId + "/likes/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(1)));

        // Add comment
        Map<String, String> commentReq = Map.of("commentText", "Incredible bass drop!");
        org.springframework.test.web.servlet.MvcResult commentResult = mockMvc.perform(post("/api/tracks/" + trackId + "/comments")
                .header("Authorization", jwtTestTokens.consumer(testArtist.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.commentText", is("Incredible bass drop!")))
                .andReturn();

        Integer parentCommentId = JsonPath.read(commentResult.getResponse().getContentAsString(), "$.data.id");

        // Reply to the comment
        Map<String, Object> replyReq = Map.of(
                "commentText", "Totally agree with you!",
                "parentCommentId", parentCommentId
        );
        mockMvc.perform(post("/api/tracks/" + trackId + "/comments")
                .header("Authorization", jwtTestTokens.consumer(testArtist.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.commentText", is("Totally agree with you!")))
                .andExpect(jsonPath("$.data.parentCommentId", is(parentCommentId)));

        // Get comments (should contain both original comment and reply)
        mockMvc.perform(get("/api/tracks/" + trackId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }
}
