package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.domain.UserProfile;
import com.soundmarketplace.sound_marketplace.dto.CreateUserRequest;
import com.soundmarketplace.sound_marketplace.dto.PublicProfileDTO;
import com.soundmarketplace.sound_marketplace.dto.UpdateProfileRequest;
import com.soundmarketplace.sound_marketplace.dto.UserAccountDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserAccountService {
    UserAccountDTO createUser(CreateUserRequest request);
    UserAccountDTO getUserById(Integer id);
    UserAccountDTO getUserByEmail(String email);
    List<UserAccountDTO> getAllUsers();
    org.springframework.data.domain.Page<UserAccountDTO> getAllUsers(org.springframework.data.domain.Pageable pageable);

    com.soundmarketplace.sound_marketplace.dto.LoginResponse login(com.soundmarketplace.sound_marketplace.dto.LoginRequest request);
    com.soundmarketplace.sound_marketplace.dto.LoginResponse refreshToken(com.soundmarketplace.sound_marketplace.dto.RefreshTokenRequest request);
    void logout(String refreshToken);

    UserProfile updateProfile(Integer userId, String displayName, String country);
    UserProfile updateProfile(Integer userId, UpdateProfileRequest request);
    byte[] getUserAvatarImage(Integer userId);
    UserProfile updateUserAvatar(Integer userId, MultipartFile avatarFile);

    void followUser(Integer followerId, Integer followedId);
    void unfollowUser(Integer followerId, Integer followedId);

    // Password & Username Recovery
    String requestPasswordReset(String email);

    /** Same as {@link #requestPasswordReset(String)} but records the requester IP for audit. */
    String requestPasswordReset(String email, String clientIp);
    void resetPassword(String token, String newPassword);
    void recoverUsername(String email);

    // Email Verification
    UserAccountDTO verifyEmail(String token);
    void resendVerificationEmail(String email);

    // SoundCloud-style Public Profile
    PublicProfileDTO getPublicProfile(Integer userId);

    // RBAC & Upload Location validation
    void validateTrackUploadEligibility(Integer uploaderUserId, Integer targetArtistId, String uploadSource);

    // User Reviews & Star Ratings
    List<com.soundmarketplace.sound_marketplace.dto.UserReviewDTO> getUserReviews(Integer targetUserId);
    Double getUserAverageRating(Integer targetUserId);
    com.soundmarketplace.sound_marketplace.dto.UserReviewDTO addUserReview(com.soundmarketplace.sound_marketplace.dto.CreateUserReviewRequest request);
    com.soundmarketplace.sound_marketplace.dto.UserReviewDTO updateUserReview(Integer reviewId, Integer reviewerId, com.soundmarketplace.sound_marketplace.dto.UpdateUserReviewRequest request);
    void deleteUserReview(Integer reviewId, Integer reviewerId);
}
