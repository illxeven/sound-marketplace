package com.soundmarketplace.sound_marketplace;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.CreatePlaylistRequest;
import com.soundmarketplace.sound_marketplace.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private TrackLikeRepository trackLikeRepository;

    @Autowired
    private TrackCommentRepository trackCommentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount testUser;
    private Track testTrack;

    @BeforeEach
    void setUp() {
        playlistRepository.deleteAll();
        trackLikeRepository.deleteAll();
        trackCommentRepository.deleteAll();
        trackRepository.deleteAll();

        testUser = userAccountRepository.findByEmail("listener_pl@soundmarketplace.com")
                .orElseGet(() -> userAccountRepository.save(UserAccount.builder()
                        .email("listener_pl@soundmarketplace.com")
                        .passwordHash("password")
                        .isActive(true)
                        .build()));

        testTrack = trackRepository.save(Track.builder()
                .artist(testUser)
                .title("Ambient Waves")
                .filePath("s3://sounds/ambient_waves.mp3")
                .genre(null)
                .build());
    }

    @Test
    @DisplayName("POST & GET /api/playlists creates, retrieves, and adds tracks to a playlist")
    void testPlaylistOperations() throws Exception {
        CreatePlaylistRequest request = CreatePlaylistRequest.builder()
                .userId(testUser.getId())
                .title("Chill Study Beats")
                .isPublic(true)
                .trackIds(List.of(testTrack.getId()))
                .build();

        String responseJson = mockMvc.perform(post("/api/playlists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Chill Study Beats")))
                .andExpect(jsonPath("$.data.tracks", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        Integer playlistId = objectMapper.readTree(responseJson).get("data").get("id").asInt();

        mockMvc.perform(get("/api/playlists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/playlists/" + playlistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("Chill Study Beats")));
    }
}
