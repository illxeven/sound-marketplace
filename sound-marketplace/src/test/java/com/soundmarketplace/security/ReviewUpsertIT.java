package com.soundmarketplace.security;

import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import com.soundmarketplace.sound_marketplace.repository.UserReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** One review per fan per artist: posting again replaces the earlier rating and comment. */
@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class ReviewUpsertIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private UserReviewRepository userReviewRepository;
    @Autowired private JwtTestTokens jwtTestTokens;

    private UserAccount user(String prefix) {
        return userAccountRepository.save(UserAccount.builder()
                .email(prefix + System.nanoTime() + "@test.com")
                .passwordHash("x").isActive(true).isEmailVerified(true).build());
    }

    @Test
    @DisplayName("Second review from the same fan updates the first instead of adding another")
    void secondReviewReplacesFirst() throws Exception {
        UserAccount fan = user("fan_");
        UserAccount artist = user("artist_");

        mockMvc.perform(post("/api/users/" + artist.getId() + "/reviews")
                        .header("Authorization", jwtTestTokens.consumer(fan.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"comment\":\"ok\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/" + artist.getId() + "/reviews")
                        .header("Authorization", jwtTestTokens.consumer(fan.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"grew on me\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        var reviews = userReviewRepository.findByTargetUserIdOrderByCreatedAtDesc(artist.getId());
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getComment()).isEqualTo("grew on me");
    }

    @Test
    @DisplayName("Ratings outside 1-5 and self-reviews are rejected")
    void invalidReviewsRejected() throws Exception {
        UserAccount fan = user("fan2_");
        UserAccount artist = user("artist2_");

        mockMvc.perform(post("/api/users/" + artist.getId() + "/reviews")
                        .header("Authorization", jwtTestTokens.consumer(fan.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/users/" + fan.getId() + "/reviews")
                        .header("Authorization", jwtTestTokens.consumer(fan.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .andExpect(status().isBadRequest());
    }
}
