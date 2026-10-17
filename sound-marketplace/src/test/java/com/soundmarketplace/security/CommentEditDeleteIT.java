package com.soundmarketplace.security;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.Role;
import com.soundmarketplace.sound_marketplace.domain.Track;
import com.soundmarketplace.sound_marketplace.domain.TrackComment;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.RoleRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackCommentRepository;
import com.soundmarketplace.sound_marketplace.repository.TrackRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class CommentEditDeleteIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private TrackCommentRepository trackCommentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTestTokens jwtTestTokens;

    private UserAccount author;
    private UserAccount stranger;
    private Track track;
    private TrackComment comment;

    private String authorToken;
    private String strangerToken;

    @BeforeEach
    void setUp() {
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));

        author = userAccountRepository.save(UserAccount.builder()
                .email("comment_author_" + System.currentTimeMillis() + "@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(consumerRole))
                .build());

        stranger = userAccountRepository.save(UserAccount.builder()
                .email("comment_stranger_" + System.currentTimeMillis() + "@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(consumerRole))
                .build());

        authorToken = jwtTestTokens.consumer(author.getId());
        strangerToken = jwtTestTokens.consumer(stranger.getId());

        track = trackRepository.save(Track.builder()
                .title("Commentable Track")
                .artist(author)
                .filePath("/tracks/test.mp3")
                .build());

        comment = trackCommentRepository.save(TrackComment.builder()
                .track(track)
                .user(author)
                .commentText("Original Comment")
                .build());
    }

    @Test
    @DisplayName("NTF-20: Stranger editing comment receives 403 Forbidden")
    void strangerCannotEditComment() throws Exception {
        Map<String, Object> updateReq = Map.of("commentText", "Hacked content");

        mockMvc.perform(put("/api/tracks/comments/" + comment.getId())
                        .header("Authorization", strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NTF-20: Author can edit own comment (200 OK)")
    void authorCanEditOwnComment() throws Exception {
        Map<String, Object> updateReq = Map.of("commentText", "Updated content by author");

        mockMvc.perform(put("/api/tracks/comments/" + comment.getId())
                        .header("Authorization", authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.commentText").value("Updated content by author"));
    }

    @Test
    @DisplayName("NTF-20: Stranger deleting comment receives 403 Forbidden")
    void strangerCannotDeleteComment() throws Exception {
        mockMvc.perform(delete("/api/tracks/comments/" + comment.getId())
                        .header("Authorization", strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NTF-20: Author can delete own comment (200 OK)")
    void authorCanDeleteOwnComment() throws Exception {
        mockMvc.perform(delete("/api/tracks/comments/" + comment.getId())
                        .header("Authorization", authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
