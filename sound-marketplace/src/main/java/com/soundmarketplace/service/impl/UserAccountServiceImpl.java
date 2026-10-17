package com.soundmarketplace.service.impl;

import com.soundmarketplace.event.PasswordResetRequestedEvent;
import com.soundmarketplace.event.UserRegisteredEvent;
import com.soundmarketplace.exception.NotFoundException;
import com.soundmarketplace.service.EmailService;
import com.soundmarketplace.service.FileStorageService;
import com.soundmarketplace.service.JwtTokenService;
import com.soundmarketplace.service.UserAccountService;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.dto.*;
import com.soundmarketplace.sound_marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
    private final com.soundmarketplace.storage.StorageService storageService;
    private final com.soundmarketplace.sound_marketplace.repository.UserReviewRepository userReviewRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final ApplicationEventPublisher eventPublisher;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String DUMMY_BCRYPT_HASH = "$2a$12$e8YkZ7G4tU57jN39W3e79e6hW1M2q4W3P8v0F.Y11bE8gA1XgB0Wy";

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static String generateRandomToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

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

        if (request.getPassword() == null || request.getPassword().length() < 10) {
            throw new IllegalArgumentException("Password must be at least 10 characters");
        }

        // NTF-04: self-selectable roles at registration are only consumer, music_artist, producer
        Set<String> allowedRoles = Set.of("consumer", "music_artist", "producer");
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String rName : request.getRoles()) {
                if (rName == null || !allowedRoles.contains(rName.trim().toLowerCase())) {
                    throw new IllegalArgumentException("Invalid role: " + rName + ". Allowed roles: consumer, music_artist, producer");
                }
            }
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            if (!allowedRoles.contains(request.getRole().trim().toLowerCase())) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole() + ". Allowed roles: consumer, music_artist, producer");
            }
        }

        String encodedPassword = passwordEncoder != null
                ? passwordEncoder.encode(request.getPassword())
                : request.getPassword();

        String rawVerificationToken = generateRandomToken();
        String tokenHash = sha256Hex(rawVerificationToken);

        UserAccount user = UserAccount.builder()
                .email(normalizedEmail)
                .passwordHash(encodedPassword)
                .isActive(true)
                .isEmailVerified(false)
                .verificationToken(tokenHash)
                .verificationTokenHash(tokenHash)
                .verificationExpiresAt(LocalDateTime.now().plusHours(24))
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
                        log.info("Recorded Terms of Service agreement for userId {} (document ID: {})", savedUser.getId(), doc.getId());
                    });
        } catch (Exception e) {
            log.error("Failed to record UserAgreement for userId: {}", savedUser.getId(), e);
        }

        String displayName = (request.getDisplayName() != null && !request.getDisplayName().isBlank())
                ? request.getDisplayName()
                : "user" + savedUser.getId();

        UserProfile profile = UserProfile.builder()
                .userAccount(savedUser)
                .displayName(displayName)
                .country(request.getCountry())
                .avatarUrl(request.getAvatarUrl())
                .avatarImageBlob(request.getAvatarImageBlob())
                .bio(request.getBio())
                .build();
        userProfileRepository.save(profile);

        // Publish event for transactional async email delivery
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getId(), savedUser.getEmail(), profile.getDisplayName(), rawVerificationToken));
        log.info("EVENT user.registered userId={}", savedUser.getId());

        return mapToDTO(savedUser, profile);
    }

    @Override
    public UserAccountDTO getUserById(Integer id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
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
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
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
    public org.springframework.data.domain.Page<UserAccountDTO> getAllUsers(org.springframework.data.domain.Pageable pageable) {
        return userAccountRepository.findAll(pageable)
                .map(u -> {
                    UserProfile p = userProfileRepository.findByUserAccountId(u.getId()).orElse(null);
                    return mapToDTO(u, p);
                });
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
        com.soundmarketplace.util.CurrentUser.requireOwnerOrAdmin(userId);
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

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
        byte[] blob = userProfileRepository.findAvatarBlobByUserId(userId);
        if (blob != null && blob.length > 0) {
            return blob;
        }
        UserProfile profile = userProfileRepository.findByUserAccountId(userId).orElse(null);
        if (profile != null && profile.getAvatarKey() != null && !profile.getAvatarKey().isBlank()) {
            return storageService.loadBytes(profile.getAvatarKey());
        }
        return null;
    }

    @Override
    @Transactional
    public UserProfile updateUserAvatar(Integer userId, MultipartFile avatarFile) {
        com.soundmarketplace.util.CurrentUser.requireOwnerOrAdmin(userId);
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
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
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<UserAccount> userOpt = userAccountRepository.findByEmail(normalizedEmail)
                .or(() -> userAccountRepository.findByEmail(request.getEmail().trim()));

        if (userOpt.isEmpty()) {
            // Timing attack protection: run dummy BCrypt check
            if (passwordEncoder != null) {
                passwordEncoder.matches("dummyPassword", DUMMY_BCRYPT_HASH);
            }
            throw new BadCredentialsException("Invalid email or password");
        }

        UserAccount user = userOpt.get();
        if (passwordEncoder != null && !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccessDeniedException("ACCOUNT_DISABLED");
        }
        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new AccessDeniedException("EMAIL_NOT_VERIFIED");
        }

        JwtTokenService.TokenPair tokenPair = jwtTokenService.generateAccessToken(user);
        String rawRefreshToken = generateRandomToken();
        String refreshHash = sha256Hex(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshHash)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(refreshToken);

        UserProfile profile = userProfileRepository.findByUserAccountId(user.getId()).orElse(null);
        log.info("EVENT auth.login.success userId={}", user.getId());

        return LoginResponse.builder()
                .accessToken(tokenPair.getToken())
                .expiresAt(tokenPair.getExpiresAt())
                .refreshToken(rawRefreshToken)
                .user(mapToDTO(user, profile))
                .build();
    }

    @Override
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        String rawToken = request.getRefreshToken().trim();
        String tokenHash = sha256Hex(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (token.isRevoked() || token.isExpired()) {
            // Reusing a rotated token revokes the whole chain!
            refreshTokenRepository.revokeAllByUserId(token.getUser().getId());
            throw new BadCredentialsException("Refresh token is revoked or expired");
        }

        // Rotate token
        String newRawRefreshToken = generateRandomToken();
        String newTokenHash = sha256Hex(newRawRefreshToken);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(token.getUser())
                .tokenHash(newTokenHash)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();
        RefreshToken savedNewToken = refreshTokenRepository.save(newRefreshToken);

        token.setRevokedAt(LocalDateTime.now());
        token.setReplacedBy(savedNewToken.getId());
        refreshTokenRepository.save(token);

        JwtTokenService.TokenPair tokenPair = jwtTokenService.generateAccessToken(token.getUser());
        UserProfile profile = userProfileRepository.findByUserAccountId(token.getUser().getId()).orElse(null);

        log.info("EVENT auth.token_refreshed userId={}", token.getUser().getId());

        return LoginResponse.builder()
                .accessToken(tokenPair.getToken())
                .expiresAt(tokenPair.getExpiresAt())
                .refreshToken(newRawRefreshToken)
                .user(mapToDTO(token.getUser(), profile))
                .build();
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hash = sha256Hex(rawRefreshToken.trim());
            refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
                rt.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(rt);
                log.info("EVENT auth.logout userId={}", rt.getUser().getId());
            });
        }
    }

    @Override
    @Transactional
    public void followUser(Integer followerId, Integer followedId) {
        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("Users cannot follow themselves");
        }
        // NTF-29: following someone twice is a no-op
        if (userRelationshipRepository.findByFollower_IdAndFollowed_Id(followerId, followedId).isPresent()) {
            return;
        }

        UserAccount follower = userAccountRepository.findById(followerId)
                .orElseThrow(() -> new NotFoundException("Follower not found with id: " + followerId));
        UserAccount followed = userAccountRepository.findById(followedId)
                .orElseThrow(() -> new NotFoundException("Followed user not found with id: " + followedId));

        UserRelationship relationship = UserRelationship.builder()
                .follower(follower)
                .followed(followed)
                .status("ACCEPTED")
                .build();
        userRelationshipRepository.save(relationship);
    }

    @Override
    @Transactional
    public void unfollowUser(Integer followerId, Integer followedId) {
        userRelationshipRepository.deleteByFollower_IdAndFollowed_Id(followerId, followedId);
    }

    @Override
    @Transactional
    public String requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return "If the email is registered, a password reset link has been sent.";
        }

        String normalized = email.trim().toLowerCase();
        Optional<UserAccount> userOpt = userAccountRepository.findByEmail(normalized)
                .or(() -> userAccountRepository.findByEmail(email.trim()));

        if (userOpt.isPresent()) {
            UserAccount user = userOpt.get();
            // Invalidate earlier tokens
            passwordResetTokenRepository.findByUserAccountId(user.getId())
                    .forEach(t -> {
                        t.setUsed(true);
                        passwordResetTokenRepository.save(t);
                    });

            String rawToken = generateRandomToken();
            String tokenHash = sha256Hex(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .userAccount(user)
                    .token(tokenHash)
                    .tokenHash(tokenHash)
                    .expiryDate(LocalDateTime.now().plusMinutes(30))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);

            eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                    user.getId(), user.getEmail(), rawToken));
            log.info("EVENT auth.password_reset.requested userId={}", user.getId());
        } else {
            log.info("Password reset requested for non-existent email");
        }

        return "If the email is registered, a password reset link has been sent.";
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Reset token is required");
        }
        if (newPassword == null || newPassword.length() < 10) {
            throw new IllegalArgumentException("Password must be at least 10 characters");
        }

        String cleanToken = token.trim();
        String tokenHash = sha256Hex(cleanToken);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .or(() -> passwordResetTokenRepository.findByToken(cleanToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

        if (Boolean.TRUE.equals(resetToken.getUsed()) || resetToken.isExpired()) {
            throw new IllegalArgumentException("Password reset token is expired or has already been used");
        }

        UserAccount user = resetToken.getUserAccount();
        String encodedPassword = passwordEncoder != null
                ? passwordEncoder.encode(newPassword)
                : newPassword;

        user.setPasswordHash(encodedPassword);
        user.setIsEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenHash(null);
        userAccountRepository.saveAndFlush(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.saveAndFlush(resetToken);

        // Revoke all refresh tokens on password reset
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("EVENT auth.password_reset.success userId={}", user.getId());
    }

    @Override
    @Transactional
    public void recoverUsername(String email) {
        if (email == null || email.isBlank()) return;
        String normalized = email.trim().toLowerCase();
        Optional<UserAccount> userOpt = userAccountRepository.findByEmail(normalized)
                .or(() -> userAccountRepository.findByEmail(email.trim()));
        if (userOpt.isEmpty()) {
            log.warn("Username recovery requested for unknown email");
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

        String cleanToken = token.trim();
        String tokenHash = sha256Hex(cleanToken);

        UserAccount user = userAccountRepository.findByVerificationTokenHash(tokenHash)
                .or(() -> userAccountRepository.findByVerificationToken(cleanToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification token"));

        if (user.getVerificationExpiresAt() != null && user.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        user.setIsEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenHash(null);
        user.setVerificationExpiresAt(null);
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

        String rawToken = generateRandomToken();
        String tokenHash = sha256Hex(rawToken);
        user.setVerificationToken(tokenHash);
        user.setVerificationTokenHash(tokenHash);
        user.setVerificationExpiresAt(LocalDateTime.now().plusHours(24));
        userAccountRepository.save(user);

        UserProfile profile = userProfileRepository.findByUserAccountId(user.getId()).orElse(null);
        String displayName = profile != null ? profile.getDisplayName() : user.getEmail().split("@")[0];

        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail(), displayName, rawToken));
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

        long likesCount = track.getLikeCount() != null ? track.getLikeCount().longValue() : 0L;
        long playsCount = track.getPlayCount() != null ? track.getPlayCount() : 0L;

        return TrackDTO.builder()
                .id(track.getId())
                .artistId(track.getArtist() != null ? track.getArtist().getId() : null)
                .artistName(track.getArtistName() != null ? track.getArtistName() :
                        (track.getArtist() != null ? track.getArtist().getEmail().split("@")[0] : null))
                .features(track.getFeatures())
                .albumId(track.getAlbum() != null ? track.getAlbum().getId() : null)
                .albumTitle(track.getAlbum() != null ? track.getAlbum().getTitle() : null)
                .title(track.getTitle())
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
                .orElseThrow(() -> new NotFoundException("Review not found with id: " + reviewId));

        com.soundmarketplace.util.CurrentUser.requireOwnerOrAdmin(review.getReviewer().getId());

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
                .orElseThrow(() -> new NotFoundException("Review not found with id: " + reviewId));

        com.soundmarketplace.util.CurrentUser.requireOwnerOrAdmin(review.getReviewer().getId());

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
                .reviewerDisplayName(reviewerName)
                .targetUserId(review.getTargetUser() != null ? review.getTargetUser().getId() : null)
                .targetUserDisplayName(targetName)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
