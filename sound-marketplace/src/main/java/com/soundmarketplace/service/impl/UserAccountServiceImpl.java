package com.soundmarketplace.service.impl;

import com.soundmarketplace.service.EmailService;
import com.soundmarketplace.service.FileStorageService;
import com.soundmarketplace.service.UserAccountService;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.dto.*;
import com.soundmarketplace.sound_marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRelationshipRepository userRelationshipRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final com.soundmarketplace.sound_marketplace.repository.TrackLikeRepository trackLikeRepository;
    private final com.soundmarketplace.sound_marketplace.repository.PlaybackHistoryRepository playbackHistoryRepository;
    private final LegalDocumentRepository legalDocumentRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final FileStorageService fileStorageService;
    private final com.soundmarketplace.sound_marketplace.repository.UserReviewRepository userReviewRepository;

    @Override
    @Transactional
    public UserAccountDTO createUser(CreateUserRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!normalizedEmail.matches(emailRegex)) {
            throw new IllegalArgumentException("Please enter a valid email address (e.g. name@example.com)");
        }
        if (request.getTermsAccepted() == null || !request.getTermsAccepted()) {
            throw new IllegalArgumentException("You must read and agree to the Terms of Service to create an account.");
        }
        if (userAccountRepository.findByEmail(normalizedEmail).isPresent() ||
            userAccountRepository.findByEmail(request.getEmail().trim()).isPresent()) {
            throw new IllegalArgumentException("An account with email " + normalizedEmail + " already exists. Please log in.");
        }

        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : "defaultPassword123";

        String encodedPassword = passwordEncoder != null
                ? passwordEncoder.encode(rawPassword)
                : rawPassword;

        String verificationToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        UserAccount user = UserAccount.builder()
                .email(normalizedEmail)
                .passwordHash(encodedPassword)
                .isActive(true)
                .isEmailVerified(false)
                .verificationToken(verificationToken)
                .build();

        // Assign roles: prioritize requested roles or default to consumer
        Set<Role> assignedRoles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String rName : request.getRoles()) {
                findRoleFlexible(rName).ifPresent(assignedRoles::add);
            }
        } else if (request.getRole() != null && !request.getRole().isBlank()) {
            findRoleFlexible(request.getRole()).ifPresent(assignedRoles::add);
        }

        if (assignedRoles.isEmpty()) {
            // Default role is consumer
            findRoleFlexible("consumer").ifPresent(assignedRoles::add);
        }
        user.setRoles(assignedRoles);

        UserAccount savedUser = userAccountRepository.save(user);

        // Record legal terms agreement
        try {
            legalDocumentRepository.findByDocumentTypes_NameIgnoreCaseAndIsActiveTrue("TERMS_OF_SERVICE").stream().findFirst()
                    .or(() -> legalDocumentRepository.findByIsActiveTrue().stream().findFirst())
                    .ifPresent(doc -> {
                        UserAgreement agreement = UserAgreement.builder()
                                .user(savedUser)
                                .document(doc)
                                .ipAddress(request.getClientIp() != null && !request.getClientIp().isBlank() ? request.getClientIp() : "127.0.0.1")
                                .agreedAt(LocalDateTime.now())
                                .build();
                        userAgreementRepository.save(agreement);
                        log.info("Recorded Terms of Service agreement for user {} (document ID: {})", savedUser.getEmail(), doc.getId());
                    });
        } catch (Exception e) {
            log.error("Failed to record UserAgreement for user: {}", savedUser.getEmail(), e);
        }

        String displayName = (request.getDisplayName() != null && !request.getDisplayName().isBlank())
                ? request.getDisplayName()
                : request.getEmail().split("@")[0];

        UserProfile profile = UserProfile.builder()
                .userAccount(savedUser)
                .displayName(displayName)
                .country(request.getCountry())
                .avatarUrl(request.getAvatarUrl())
                .avatarImageBlob(request.getAvatarImageBlob())
                .bio(request.getBio())
                .build();
        userProfileRepository.save(profile);

        // Send email verification notification safely
        emailService.sendVerificationEmail(savedUser, displayName, verificationToken);

        return mapToDTO(savedUser, profile);
    }

    @Override
    public UserAccountDTO getUserById(Integer id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        UserProfile profile = userProfileRepository.findByUserAccountId(id).orElse(null);
        return mapToDTO(user, profile);
    }

    @Override
    public UserAccountDTO getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalized = email.trim().toLowerCase();
        UserAccount user = userAccountRepository.findByEmail(normalized)
                .or(() -> userAccountRepository.findByEmail(email.trim()))
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        UserProfile profile = userProfileRepository.findByUserAccountId(user.getId()).orElse(null);
        return mapToDTO(user, profile);
    }

    @Override
    public List<UserAccountDTO> getAllUsers() {
        return userAccountRepository.findAll().stream()
                .map(u -> {
                    UserProfile p = userProfileRepository.findByUserAccountId(u.getId()).orElse(null);
                    return mapToDTO(u, p);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserProfile updateProfile(Integer userId, String displayName, String country) {
        return updateProfile(userId, UpdateProfileRequest.builder()
                .displayName(displayName)
                .country(country)
                .build());
    }

    @Override
    @Transactional
    public UserProfile updateProfile(Integer userId, UpdateProfileRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElse(UserProfile.builder().userAccount(user).build());

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            profile.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getCountry() != null) {
            profile.setCountry(request.getCountry());
        }
        if (request.getAvatarImageBlob() != null) {
            profile.setAvatarImageBlob(request.getAvatarImageBlob());
        }

        return userProfileRepository.save(profile);
    }

    @Override
    public byte[] getUserAvatarImage(Integer userId) {
        UserProfile profile = userProfileRepository.findByUserAccountId(userId).orElse(null);
        if (profile != null && profile.getAvatarImageBlob() != null && profile.getAvatarImageBlob().length > 0) {
            return profile.getAvatarImageBlob();
        }
        return null;
    }

    @Override
    @Transactional
    public UserProfile updateUserAvatar(Integer userId, MultipartFile avatarFile) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElse(UserProfile.builder().userAccount(user).displayName(user.getEmail().split("@")[0]).build());

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                profile.setAvatarImageBlob(avatarFile.getBytes());
                String artPath = fileStorageService.storeFile(avatarFile, "artwork");
                profile.setAvatarUrl(artPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read avatar file", e);
            }
        }
        return userProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public void followUser(Integer followerId, Integer followedId) {
        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("Users cannot follow themselves");
        }
        UserAccount follower = userAccountRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException("Follower not found"));
        UserAccount followed = userAccountRepository.findById(followedId)
                .orElseThrow(() -> new IllegalArgumentException("Followed user not found"));

        UserRelationship relationship = UserRelationship.builder()
                .follower(follower)
                .followed(followed)
                .status("ACCEPTED")
                .build();
        userRelationshipRepository.save(relationship);
    }

    @Override
    @Transactional
    public String requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalized = email.trim().toLowerCase();
        Optional<UserAccount> userOpt = userAccountRepository.findByEmail(normalized)
                .or(() -> userAccountRepository.findByEmail(email.trim()));
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", email);
            throw new IllegalArgumentException("Email is not found in the db");
        }

        UserAccount user = userOpt.get();
        // Generate an 8-character uppercase alphanumeric token (clean and user-friendly)
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userAccount(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getId(), token);
        log.info("Password reset token generated for user: {} (Token: {})", user.getEmail(), token);
        return token;
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Reset token is required");
        }
        String cleanToken = token.trim();
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(cleanToken)
                .or(() -> passwordResetTokenRepository.findByTokenIgnoreCase(cleanToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

        if (Boolean.TRUE.equals(resetToken.getUsed()) || resetToken.isExpired()) {
            throw new IllegalArgumentException("Password reset token is expired or has already been used");
        }

        UserAccount user = resetToken.getUserAccount();
        String encodedPassword = passwordEncoder != null
                ? passwordEncoder.encode(newPassword)
                : newPassword;

        user.setPasswordHash(encodedPassword);
        // Resetting password via email token confirms email ownership, activating user account
        user.setIsEmailVerified(true);
        user.setVerificationToken(null);
        userAccountRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset successfully and email verified for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void recoverUsername(String email) {
        if (email == null || email.isBlank()) return;
        String normalized = email.trim().toLowerCase();
        Optional<UserAccount> userOpt = userAccountRepository.findByEmail(normalized)
                .or(() -> userAccountRepository.findByEmail(email.trim()));
        if (userOpt.isEmpty()) {
            log.warn("Username recovery requested for unknown email: {}", email);
            return;
        }

        UserAccount user = userOpt.get();
        UserProfile profile = userProfileRepository.findByUserAccountId(user.getId()).orElse(null);
        String username = profile != null ? profile.getDisplayName() : user.getEmail();

        emailService.sendUsernameRecoveryEmail(user.getEmail(), user.getId(), username);
    }

    @Override
    @Transactional
    public UserAccountDTO verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Verification token is required");
        }

        UserAccount user = userAccountRepository.findByVerificationToken(token.trim().toUpperCase())
                .or(() -> userAccountRepository.findByVerificationToken(token.trim()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification token"));

        user.setIsEmailVerified(true);
        user.setVerificationToken(null);
        UserAccount saved = userAccountRepository.save(user);

        UserProfile profile = userProfileRepository.findByUserAccountId(saved.getId()).orElse(null);
        log.info("Email verified successfully for user: {}", saved.getEmail());
        return mapToDTO(saved, profile);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalized = email.trim().toLowerCase();
        UserAccount user = userAccountRepository.findByEmail(normalized)
                .or(() -> userAccountRepository.findByEmail(email.trim()))
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            log.info("Email already verified for user: {}", email);
            return;
        }

        String newToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        user.setVerificationToken(newToken);
        userAccountRepository.save(user);

        UserProfile profile = userProfileRepository.findByUserAccountId(user.getId()).orElse(null);
        String displayName = profile != null ? profile.getDisplayName() : user.getEmail().split("@")[0];

        emailService.sendVerificationEmail(user, displayName, newToken);
        log.info("Resent verification email for user: {}", email);
    }

    @Override
    public PublicProfileDTO getPublicProfile(Integer userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        UserProfile profile = userProfileRepository.findByUserAccountId(userId).orElse(null);

        List<TrackDTO> tracks = trackRepository.findByArtistId(userId).stream()
                .map(this::mapTrackToDTO)
                .collect(Collectors.toList());

        List<AlbumDTO> albums = albumRepository.findByArtistId(userId).stream()
                .map(this::mapAlbumToDTO)
                .collect(Collectors.toList());

        long followers = userRelationshipRepository.countByFollowedId(userId);
        long following = userRelationshipRepository.countByFollowerId(userId);

        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toList())
                : List.of();

        boolean hasAvatar = profile != null && profile.getAvatarImageBlob() != null && profile.getAvatarImageBlob().length > 0;
        String avatarUrl = hasAvatar
                ? "/api/users/" + user.getId() + "/avatar"
                : (profile != null ? profile.getAvatarUrl() : null);

        return PublicProfileDTO.builder()
                .userId(user.getId())
                .displayName(profile != null ? profile.getDisplayName() : "Artist")
                .bio(profile != null ? profile.getBio() : null)
                .avatarUrl(avatarUrl)
                .hasAvatarImage(hasAvatar)
                .country(profile != null ? profile.getCountry() : null)
                .roles(roleNames)
                .tracks(tracks)
                .albums(albums)
                .followersCount(followers)
                .followingCount(following)
                .joinedAt(user.getCreatedAt())
                .build();
    }

    @Override
    public void validateTrackUploadEligibility(Integer uploaderUserId, Integer targetArtistId, String uploadSource) {
        // Rule 1: Upload Location Rule: Music uploading must happen exclusively from the user's own profile page, never from the discover page
        if ("DISCOVER_PAGE".equalsIgnoreCase(uploadSource)) {
            throw new IllegalStateException("Music uploading must happen exclusively from the user's own profile page, never from the discover page.");
        }

        if (uploaderUserId != null) {
            UserAccount uploader = userAccountRepository.findById(uploaderUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Uploader user not found with id: " + uploaderUserId));

            // Rule 2: Track Upload Permissions: Only logged-in users with roles ADMIN, SUPER_ADMIN, MUSIC_ARTIST, or PRODUCER are allowed to upload tracks
            if (!uploader.canUploadTracks()) {
                throw new SecurityException("Consumers cannot upload music. Only users with roles ADMIN, SUPER_ADMIN, MUSIC_ARTIST, or PRODUCER are allowed to upload tracks.");
            }

            // Rule 3: Must be on the artist's own profile page
            if (targetArtistId != null && !uploader.hasRole("ADMIN") && !uploader.hasRole("SUPER_ADMIN") && !uploaderUserId.equals(targetArtistId)) {
                throw new SecurityException("Music uploading must happen exclusively from the user's own profile page.");
            }
        }
    }

    private UserAccountDTO mapToDTO(UserAccount user, UserProfile profile) {
        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toList())
                : List.of();

        boolean hasAvatar = profile != null && profile.getAvatarImageBlob() != null && profile.getAvatarImageBlob().length > 0;
        String avatarUrl = hasAvatar
                ? "/api/users/" + user.getId() + "/avatar"
                : (profile != null ? profile.getAvatarUrl() : null);

        return UserAccountDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .displayName(profile != null ? profile.getDisplayName() : null)
                .bio(profile != null ? profile.getBio() : null)
                .avatarUrl(avatarUrl)
                .hasAvatarImage(hasAvatar)
                .country(profile != null ? profile.getCountry() : null)
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private TrackDTO mapTrackToDTO(Track track) {
        boolean hasCover = track.getCoverImageBlob() != null && track.getCoverImageBlob().length > 0;
        String coverUrl = hasCover
                ? "/api/tracks/" + track.getId() + "/cover"
                : (track.getAlbum() != null && track.getAlbum().getCoverArtUrl() != null ? track.getAlbum().getCoverArtUrl() : null);

        long likesCount = track.getId() != null ? trackLikeRepository.countByTrackId(track.getId()) : 0L;
        long playsCount = track.getId() != null ? playbackHistoryRepository.countByTrackId(track.getId()) : 0L;

        return TrackDTO.builder()
                .id(track.getId())
                .artistId(track.getArtist() != null ? track.getArtist().getId() : null)
                .artistEmail(track.getArtist() != null ? track.getArtist().getEmail() : null)
                .artistName(track.getArtistName() != null ? track.getArtistName() :
                        (track.getArtist() != null ? track.getArtist().getEmail().split("@")[0] : null))
                .features(track.getFeatures())
                .albumId(track.getAlbum() != null ? track.getAlbum().getId() : null)
                .albumTitle(track.getAlbum() != null ? track.getAlbum().getTitle() : null)
                .title(track.getTitle())
                .filePath(track.getFilePath())
                .genre(track.getGenre() != null ? track.getGenre().getName() : null)
                .durationSeconds(track.getDurationSeconds())
                .isPromoted(track.getIsPromoted())
                .price(track.getPrice())
                .hasCoverImage(hasCover)
                .coverImageUrl(coverUrl)
                .likesCount(likesCount)
                .playsCount(playsCount)
                .createdAt(track.getCreatedAt())
                .build();
    }

    private AlbumDTO mapAlbumToDTO(Album album) {
        boolean hasCover = album.getCoverImageBlob() != null && album.getCoverImageBlob().length > 0;
        String coverUrl = hasCover ? "/api/albums/" + album.getId() + "/cover" : album.getCoverArtUrl();

        return AlbumDTO.builder()
                .id(album.getId())
                .artistId(album.getArtist() != null ? album.getArtist().getId() : null)
                .artistEmail(album.getArtist() != null ? album.getArtist().getEmail() : null)
                .title(album.getTitle())
                .coverArtUrl(coverUrl)
                .hasCoverImage(hasCover)
                .coverImageUrl(coverUrl)
                .createdAt(album.getCreatedAt())
                .build();
    }

    private Optional<Role> findRoleFlexible(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String clean = name.trim();
        Optional<Role> exact = roleRepository.findByRoleNameIgnoreCase(clean);
        if (exact.isPresent()) return exact;

        if (clean.toUpperCase().startsWith("ROLE_")) {
            String stripped = clean.substring(5);
            exact = roleRepository.findByRoleNameIgnoreCase(stripped);
            if (exact.isPresent()) return exact;
        } else {
            exact = roleRepository.findByRoleNameIgnoreCase("ROLE_" + clean);
            if (exact.isPresent()) return exact;
        }

        String targetClean = clean.toLowerCase().replace("role_", "").replace("_", "");
        return roleRepository.findAll().stream()
                .filter(r -> r.getRoleName() != null &&
                        r.getRoleName().toLowerCase().replace("role_", "").replace("_", "").equals(targetClean))
                .findFirst();
    }

    @Override
    public List<com.soundmarketplace.sound_marketplace.dto.UserReviewDTO> getUserReviews(Integer targetUserId) {
        return userReviewRepository.findByTargetUserIdOrderByCreatedAtDesc(targetUserId).stream()
                .map(this::mapUserReviewToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Double getUserAverageRating(Integer targetUserId) {
        Double avg = userReviewRepository.getAverageRatingByTargetUserId(targetUserId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : null;
    }

    @Override
    @Transactional
    public com.soundmarketplace.sound_marketplace.dto.UserReviewDTO addUserReview(com.soundmarketplace.sound_marketplace.dto.CreateUserReviewRequest request) {
        UserAccount reviewer = userAccountRepository.findById(request.getReviewerId())
                .orElseThrow(() -> new IllegalArgumentException("Reviewer user not found with id: " + request.getReviewerId()));
        UserAccount target = userAccountRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new IllegalArgumentException("Target artist/user not found with id: " + request.getTargetUserId()));

        if (reviewer.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Users cannot submit reviews for their own profile");
        }

        int rating = Math.min(5, Math.max(1, request.getRating()));
        com.soundmarketplace.sound_marketplace.domain.UserReview review = com.soundmarketplace.sound_marketplace.domain.UserReview.builder()
                .reviewer(reviewer)
                .targetUser(target)
                .rating(rating)
                .comment(request.getComment() != null ? request.getComment().trim() : null)
                .build();

        com.soundmarketplace.sound_marketplace.domain.UserReview saved = userReviewRepository.save(review);
        return mapUserReviewToDTO(saved);
    }

    @Override
    @Transactional
    public com.soundmarketplace.sound_marketplace.dto.UserReviewDTO updateUserReview(Integer reviewId, Integer reviewerId, com.soundmarketplace.sound_marketplace.dto.UpdateUserReviewRequest request) {
        com.soundmarketplace.sound_marketplace.domain.UserReview review = userReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with id: " + reviewId));

        if (!review.getReviewer().getId().equals(reviewerId)) {
            throw new IllegalArgumentException("Only the author can edit this review");
        }

        if (request.getRating() != null) {
            review.setRating(Math.min(5, Math.max(1, request.getRating())));
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment().trim());
        }

        com.soundmarketplace.sound_marketplace.domain.UserReview saved = userReviewRepository.save(review);
        return mapUserReviewToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteUserReview(Integer reviewId, Integer reviewerId) {
        com.soundmarketplace.sound_marketplace.domain.UserReview review = userReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with id: " + reviewId));

        if (!review.getReviewer().getId().equals(reviewerId)) {
            throw new IllegalArgumentException("Only the author can delete this review");
        }

        userReviewRepository.delete(review);
    }

    private com.soundmarketplace.sound_marketplace.dto.UserReviewDTO mapUserReviewToDTO(com.soundmarketplace.sound_marketplace.domain.UserReview review) {
        String reviewerName = "Music Fan";
        if (review.getReviewer() != null) {
            reviewerName = userProfileRepository.findByUserAccountId(review.getReviewer().getId())
                    .map(UserProfile::getDisplayName)
                    .filter(n -> !n.trim().isEmpty())
                    .orElse(review.getReviewer().getEmail().split("@")[0]);
        }
        String targetName = "Artist";
        if (review.getTargetUser() != null) {
            targetName = userProfileRepository.findByUserAccountId(review.getTargetUser().getId())
                    .map(UserProfile::getDisplayName)
                    .filter(n -> !n.trim().isEmpty())
                    .orElse(review.getTargetUser().getEmail().split("@")[0]);
        }

        return com.soundmarketplace.sound_marketplace.dto.UserReviewDTO.builder()
                .id(review.getId())
                .reviewerId(review.getReviewer() != null ? review.getReviewer().getId() : null)
                .reviewerEmail(review.getReviewer() != null ? review.getReviewer().getEmail() : null)
                .reviewerDisplayName(reviewerName)
                .targetUserId(review.getTargetUser() != null ? review.getTargetUser().getId() : null)
                .targetUserDisplayName(targetName)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
