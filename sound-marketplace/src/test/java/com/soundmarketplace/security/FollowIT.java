package com.soundmarketplace.security;

import com.soundmarketplace.sound_marketplace.domain.Role;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.RoleRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class FollowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTestTokens jwtTestTokens;

    @Autowired
    private com.soundmarketplace.sound_marketplace.repository.UserRelationshipRepository userRelationshipRepository;

    private UserAccount userA;
    private UserAccount userB;
    private String tokenA;

    @BeforeEach
    void setUp() {
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));

        userA = userAccountRepository.save(UserAccount.builder()
                .email("follow_a_" + System.currentTimeMillis() + "@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(consumerRole))
                .build());

        userB = userAccountRepository.save(UserAccount.builder()
                .email("follow_b_" + System.currentTimeMillis() + "@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(consumerRole))
                .build());

        tokenA = jwtTestTokens.consumer(userA.getId());
    }

    @Test
    @DisplayName("NTF-29: Following twice is a no-op; unfollow works; self-follow fails with 400")
    void followLifecycle() throws Exception {
        // 1. Self-follow must fail with 400
        mockMvc.perform(post("/api/users/me/follow/" + userA.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 2. First follow: succeeds
        mockMvc.perform(post("/api/users/me/follow/" + userB.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(userRelationshipRepository.findByFollower_IdAndFollowed_Id(userA.getId(), userB.getId())).isPresent();

        // 3. Second follow (duplicate): no-op, still 1 relation
        mockMvc.perform(post("/api/users/me/follow/" + userB.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk());

        assertThat(userRelationshipRepository.findByFollower_IdAndFollowed_Id(userA.getId(), userB.getId())).isPresent();

        // 4. Unfollow: removes row
        mockMvc.perform(delete("/api/users/me/follow/" + userB.getId())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(userRelationshipRepository.findByFollower_IdAndFollowed_Id(userA.getId(), userB.getId())).isEmpty();
    }
}
