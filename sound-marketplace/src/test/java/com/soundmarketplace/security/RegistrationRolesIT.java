package com.soundmarketplace.security;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.Role;
import com.soundmarketplace.sound_marketplace.dto.CreateUserRequest;
import com.soundmarketplace.sound_marketplace.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class RegistrationRolesIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));
        roleRepository.findByRoleNameIgnoreCase("music_artist")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("music_artist").build()));
        roleRepository.findByRoleNameIgnoreCase("producer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("producer").build()));
    }

    @Test
    @DisplayName("NTF-04: Admin and SuperAdmin roles give 400 at registration")
    void forbiddenRolesGive400() throws Exception {
        CreateUserRequest req1 = CreateUserRequest.builder()
                .email("hacker_admin_" + System.currentTimeMillis() + "@test.com")
                .password("StrongPassword123!")
                .role("admin")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        CreateUserRequest req2 = CreateUserRequest.builder()
                .email("hacker_super_" + System.currentTimeMillis() + "@test.com")
                .password("StrongPassword123!")
                .roles(List.of("super_admin"))
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("NTF-04: Unknown role gives 400 at registration")
    void unknownRoleGives400() throws Exception {
        CreateUserRequest req = CreateUserRequest.builder()
                .email("unknown_role_" + System.currentTimeMillis() + "@test.com")
                .password("StrongPassword123!")
                .role("arbitrary_role")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("NTF-04: Password < 10 characters or missing gives 400")
    void invalidPasswordGives400() throws Exception {
        // Short password
        CreateUserRequest reqShort = CreateUserRequest.builder()
                .email("short_pw_" + System.currentTimeMillis() + "@test.com")
                .password("short123")
                .role("consumer")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqShort)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Missing password
        CreateUserRequest reqMissing = CreateUserRequest.builder()
                .email("missing_pw_" + System.currentTimeMillis() + "@test.com")
                .role("consumer")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqMissing)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("NTF-04: Allowed roles (consumer, music_artist, producer) succeed")
    void allowedRolesSucceed() throws Exception {
        for (String allowedRole : List.of("consumer", "music_artist", "producer")) {
            CreateUserRequest req = CreateUserRequest.builder()
                    .email("valid_" + allowedRole + "_" + System.currentTimeMillis() + "@test.com")
                    .password("ValidPassword123!")
                    .role(allowedRole)
                    .termsAccepted(true)
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
