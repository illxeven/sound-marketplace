package com.soundmarketplace.controller;

import com.soundmarketplace.service.UserAccountService;
import com.soundmarketplace.sound_marketplace.domain.UserProfile;
import com.soundmarketplace.sound_marketplace.dto.*;
import com.soundmarketplace.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final com.soundmarketplace.service.EngagementService engagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserAccountDTO>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest servletRequest) {
        if (request.getClientIp() == null || request.getClientIp().isBlank()) {
            request.setClientIp(extractClientIp(servletRequest));
        }
        UserAccountDTO user = userAccountService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User created successfully", user));
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserAccountDTO>>> getAllUsers(Pageable pageable) {
        Page<UserAccountDTO> users = userAccountService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserAccountDTO>> getCurrentUser() {
        Integer currentUserId = CurrentUser.getUserId();
        UserAccountDTO user = userAccountService.getUserById(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    // forgot-password / reset-password live only in AuthController (/api/auth/...), which is rate-limited.


    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfile>> updateCurrentProfile(@RequestBody Map<String, String> body) {
        Integer currentUserId = CurrentUser.getUserId();
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .displayName(body.get("displayName"))
                .bio(body.get("bio"))
                .avatarUrl(body.get("avatarUrl"))
                .country(body.get("country"))
                .build();
        UserProfile profile = userAccountService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", profile));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<UserProfile>> updateProfile(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        CurrentUser.requireOwnerOrAdmin(id);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .displayName(body.get("displayName"))
                .bio(body.get("bio"))
                .avatarUrl(body.get("avatarUrl"))
                .country(body.get("country"))
                .build();
        UserProfile profile = userAccountService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", profile));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfile>> uploadCurrentAvatar(@RequestParam("avatar") MultipartFile avatar) {
        Integer currentUserId = CurrentUser.getUserId();
        UserProfile profile = userAccountService.updateUserAvatar(currentUserId, avatar);
        return ResponseEntity.ok(ApiResponse.ok("Avatar updated successfully", profile));
    }

    @PostMapping("/me/follow/{targetId}")
    public ResponseEntity<ApiResponse<Void>> followUser(@PathVariable Integer targetId) {
        Integer currentUserId = CurrentUser.getUserId();
        userAccountService.followUser(currentUserId, targetId);
        return ResponseEntity.ok(ApiResponse.ok("Successfully followed user", null));
    }

    @DeleteMapping("/me/follow/{targetId}")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(@PathVariable Integer targetId) {
        Integer currentUserId = CurrentUser.getUserId();
        userAccountService.unfollowUser(currentUserId, targetId);
        return ResponseEntity.ok(ApiResponse.ok("Successfully unfollowed user", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserAccountDTO>> getUserById(@PathVariable Integer id) {
        UserAccountDTO user = userAccountService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getUserAvatar(
            @PathVariable Integer id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        byte[] imageBytes = userAccountService.getUserAvatarImage(id);
        return com.soundmarketplace.util.ImageResponseUtil.createCacheableImageResponse(imageBytes, ifNoneMatch);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserAccountDTO>> getUserByEmail(@PathVariable String email) {
        UserAccountDTO user = userAccountService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @GetMapping("/{id}/public-profile")
    public ResponseEntity<ApiResponse<PublicProfileDTO>> getPublicProfile(@PathVariable Integer id) {
        PublicProfileDTO profile = userAccountService.getPublicProfile(id);
        return ResponseEntity.ok(ApiResponse.ok("Public profile retrieved successfully", profile));
    }

    /** Track ids the signed-in user has liked (for heart state on every screen). */
    @GetMapping("/me/liked-track-ids")
    public ResponseEntity<ApiResponse<List<Integer>>> getMyLikedTrackIds() {
        return ResponseEntity.ok(ApiResponse.ok(engagementService.getLikedTrackIds(CurrentUser.getUserId())));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<List<com.soundmarketplace.sound_marketplace.dto.UserReviewDTO>>> getUserReviews(@PathVariable Integer id) {
        List<com.soundmarketplace.sound_marketplace.dto.UserReviewDTO> reviews = userAccountService.getUserReviews(id);
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }

    @GetMapping("/{id}/rating")
    public ResponseEntity<ApiResponse<Double>> getUserAverageRating(@PathVariable Integer id) {
        Double rating = userAccountService.getUserAverageRating(id);
        return ResponseEntity.ok(ApiResponse.ok(rating));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<com.soundmarketplace.sound_marketplace.dto.UserReviewDTO>> addUserReview(
            @PathVariable Integer id,
            @Valid @RequestBody com.soundmarketplace.sound_marketplace.dto.CreateUserReviewRequest request) {
        request.setTargetUserId(id);
        request.setReviewerId(CurrentUser.getUserId());
        com.soundmarketplace.sound_marketplace.dto.UserReviewDTO review = userAccountService.addUserReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Review submitted successfully", review));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<com.soundmarketplace.sound_marketplace.dto.UserReviewDTO>> updateUserReview(
            @PathVariable Integer reviewId,
            @Valid @RequestBody com.soundmarketplace.sound_marketplace.dto.UpdateUserReviewRequest request) {
        Integer currentUserId = CurrentUser.getUserId();
        com.soundmarketplace.sound_marketplace.dto.UserReviewDTO review = userAccountService.updateUserReview(reviewId, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("Review updated successfully", review));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteUserReview(@PathVariable Integer reviewId) {
        Integer currentUserId = CurrentUser.getUserId();
        userAccountService.deleteUserReview(reviewId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted successfully", null));
    }
}
