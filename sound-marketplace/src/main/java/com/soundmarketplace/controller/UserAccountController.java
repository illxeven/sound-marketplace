package com.soundmarketplace.controller;

import com.soundmarketplace.service.UserAccountService;
import com.soundmarketplace.sound_marketplace.domain.UserProfile;
import com.soundmarketplace.sound_marketplace.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserAccountController {

    private final UserAccountService userAccountService;

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
    public ResponseEntity<ApiResponse<List<UserAccountDTO>>> getAllUsers() {
        List<UserAccountDTO> users = userAccountService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserAccountDTO>> getUserById(@PathVariable Integer id) {
        UserAccountDTO user = userAccountService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable Integer id) {
        byte[] imageBytes = userAccountService.getUserAvatarImage(id);
        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfile>> uploadUserAvatar(
            @PathVariable Integer id,
            @RequestParam("avatar") MultipartFile avatar) {
        UserProfile profile = userAccountService.updateUserAvatar(id, avatar);
        return ResponseEntity.ok(ApiResponse.ok("Avatar updated successfully", profile));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserAccountDTO>> getUserByEmail(@PathVariable String email) {
        UserAccountDTO user = userAccountService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<UserProfile>> updateProfile(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .displayName(body.get("displayName"))
                .bio(body.get("bio"))
                .avatarUrl(body.get("avatarUrl"))
                .country(body.get("country"))
                .build();
        UserProfile profile = userAccountService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", profile));
    }

    @GetMapping("/{id}/public-profile")
    public ResponseEntity<ApiResponse<PublicProfileDTO>> getPublicProfile(@PathVariable Integer id) {
        PublicProfileDTO profile = userAccountService.getPublicProfile(id);
        return ResponseEntity.ok(ApiResponse.ok("Public profile retrieved successfully", profile));
    }

    @PostMapping("/{id}/follow/{targetId}")
    public ResponseEntity<ApiResponse<Void>> followUser(
            @PathVariable Integer id,
            @PathVariable Integer targetId) {
        userAccountService.followUser(id, targetId);
        return ResponseEntity.ok(ApiResponse.ok("Successfully followed user", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = userAccountService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("If the email exists, a password reset link has been dispatched.",
                java.util.Map.of("token", token, "email", request.getEmail())));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userAccountService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully.", null));
    }

    @PostMapping("/recover-username")
    public ResponseEntity<ApiResponse<Void>> recoverUsername(@RequestParam String email) {
        userAccountService.recoverUsername(email);
        return ResponseEntity.ok(ApiResponse.ok("If the email exists, your username reminder has been sent.", null));
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
        com.soundmarketplace.sound_marketplace.dto.UserReviewDTO review = userAccountService.addUserReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Review submitted successfully", review));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<com.soundmarketplace.sound_marketplace.dto.UserReviewDTO>> updateUserReview(
            @PathVariable Integer reviewId,
            @RequestParam Integer reviewerId,
            @Valid @RequestBody com.soundmarketplace.sound_marketplace.dto.UpdateUserReviewRequest request) {
        com.soundmarketplace.sound_marketplace.dto.UserReviewDTO review = userAccountService.updateUserReview(reviewId, reviewerId, request);
        return ResponseEntity.ok(ApiResponse.ok("Review updated successfully", review));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteUserReview(
            @PathVariable Integer reviewId,
            @RequestParam Integer reviewerId) {
        userAccountService.deleteUserReview(reviewId, reviewerId);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted successfully", null));
    }
}
