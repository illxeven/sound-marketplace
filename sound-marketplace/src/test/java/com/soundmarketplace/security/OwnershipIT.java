package com.soundmarketplace.security;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.dto.CreateAlbumRequest;
import com.soundmarketplace.sound_marketplace.dto.CreatePlaylistRequest;
import com.soundmarketplace.sound_marketplace.dto.CreateTrackRequest;
import com.soundmarketplace.sound_marketplace.dto.UpdateCartItemRequest;
import com.soundmarketplace.sound_marketplace.dto.UpdateMerchandiseRequest;
import com.soundmarketplace.sound_marketplace.dto.UpdateUserReviewRequest;
import com.soundmarketplace.sound_marketplace.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class OwnershipIT {

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
    private AlbumRepository albumRepository;

    @Autowired
    private MerchandiseRepository merchandiseRepository;

    @Autowired
    private MerchandiseCategoryRepository merchandiseCategoryRepository;

    @Autowired
    private com.soundmarketplace.sound_marketplace.repository.MerchandiseGroupTypeRepository merchandiseGroupTypeRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private TrackCommentRepository trackCommentRepository;

    @Autowired
    private UserReviewRepository userReviewRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTestTokens jwtTestTokens;

    private UserAccount userA;
    private UserAccount userB;
    private UserAccount adminUser;

    private String userAToken;
    private String userBToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        Role artistRole = roleRepository.findByRoleNameIgnoreCase("music_artist")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("music_artist").build()));
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));
        Role adminRole = roleRepository.findByRoleNameIgnoreCase("admin")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("admin").build()));

        userA = userAccountRepository.save(UserAccount.builder()
                .email("owner_a_" + System.currentTimeMillis() + "@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(artistRole, consumerRole))
                .build());

        userB = userAccountRepository.save(UserAccount.builder()
                .email("owner_b_" + System.currentTimeMillis() + "@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(artistRole, consumerRole))
                .build());

        adminUser = userAccountRepository.save(UserAccount.builder()
                .email("admin_" + System.currentTimeMillis() + "@test.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(adminRole))
                .build());

        userAToken = jwtTestTokens.artist(userA.getId());
        userBToken = jwtTestTokens.artist(userB.getId());
        adminToken = jwtTestTokens.admin(adminUser.getId());
    }

    @Test
    @DisplayName("Track ownership: user B cannot modify user A's track (403), admin can (200)")
    void trackOwnership() throws Exception {
        Track track = trackRepository.save(Track.builder()
                .artist(userA)
                .title("Track A")
                .filePath("/tracks/testA.mp3")
                .price(BigDecimal.valueOf(1.99))
                .build());

        CreateTrackRequest updateReq = CreateTrackRequest.builder()
                .title("Hacked Title")
                .build();

        // User B gets 403
        mockMvc.perform(put("/api/tracks/" + track.getId())
                        .header("Authorization", userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // Admin gets 200
        mockMvc.perform(put("/api/tracks/" + track.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Album ownership: user B cannot modify user A's album (403), admin can (200)")
    void albumOwnership() throws Exception {
        Album album = albumRepository.save(Album.builder()
                .artist(userA)
                .title("Album A")
                .price(BigDecimal.valueOf(9.99))
                .build());

        CreateAlbumRequest updateReq = CreateAlbumRequest.builder()
                .title("Updated Album Title")
                .build();

        // User B gets 403
        mockMvc.perform(put("/api/albums/" + album.getId())
                        .header("Authorization", userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // Admin gets 200
        mockMvc.perform(put("/api/albums/" + album.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Merchandise ownership: user B cannot modify user A's merchandise (403), admin can (200)")
    void merchandiseOwnership() throws Exception {
        MerchandiseCategory category = merchandiseCategoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    MerchandiseGroupType groupType = merchandiseGroupTypeRepository.findAll().stream().findFirst()
                            .orElseGet(() -> merchandiseGroupTypeRepository.save(MerchandiseGroupType.builder().name("Physical").build()));
                    return merchandiseCategoryRepository.save(MerchandiseCategory.builder()
                            .name("Apparel")
                            .groupType(groupType)
                            .build());
                });

        Merchandise merch = merchandiseRepository.save(Merchandise.builder()
                .seller(userA)
                .category(category)
                .title("T-Shirt")
                .price(BigDecimal.valueOf(25.00))
                .build());

        UpdateMerchandiseRequest updateReq = UpdateMerchandiseRequest.builder()
                .title("Hacked Merch")
                .price(BigDecimal.valueOf(15.00))
                .build();

        // User B gets 403
        mockMvc.perform(put("/api/merchandise/" + merch.getId())
                        .header("Authorization", userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // Admin gets 200
        mockMvc.perform(put("/api/merchandise/" + merch.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Playlist ownership: user B cannot modify user A's playlist (403), admin can (200)")
    void playlistOwnership() throws Exception {
        Playlist playlist = playlistRepository.save(Playlist.builder()
                .user(userA)
                .title("Playlist A")
                .isPublic(true)
                .build());

        CreatePlaylistRequest updateReq = CreatePlaylistRequest.builder()
                .title("Updated Playlist")
                .isPublic(false)
                .build();

        // User B gets 403
        mockMvc.perform(put("/api/playlists/" + playlist.getId())
                        .header("Authorization", userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // Admin gets 200
        mockMvc.perform(put("/api/playlists/" + playlist.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UserReview ownership: user B cannot update user A's review (403), admin can (200)")
    void userReviewOwnership() throws Exception {
        UserReview review = userReviewRepository.save(UserReview.builder()
                .reviewer(userA)
                .targetUser(userB)
                .rating(5)
                .comment("Great artist")
                .build());

        UpdateUserReviewRequest updateReq = UpdateUserReviewRequest.builder()
                .rating(1)
                .comment("Terrible")
                .build();

        // User B gets 403
        mockMvc.perform(put("/api/users/reviews/" + review.getId())
                        .header("Authorization", userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // Admin gets 200
        mockMvc.perform(put("/api/users/reviews/" + review.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CartItem ownership: user B cannot modify user A's cart item (403), admin can (200)")
    void cartItemOwnership() throws Exception {
        Cart cartA = cartRepository.save(Cart.builder().user(userA).build());
        Track track = trackRepository.save(Track.builder().artist(userA).title("Track C").filePath("/tracks/testC.mp3").build());
        CartItem cartItem = cartItemRepository.save(CartItem.builder()
                .cart(cartA)
                .track(track)
                .quantity(1)
                .priceSnapshot(BigDecimal.valueOf(1.99))
                .build());

        UpdateCartItemRequest updateReq = UpdateCartItemRequest.builder()
                .quantity(5)
                .build();

        // User B gets 403
        mockMvc.perform(put("/api/cart/items/" + cartItem.getId())
                        .header("Authorization", userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // Admin gets 200
        mockMvc.perform(put("/api/cart/items/" + cartItem.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());
    }
}
