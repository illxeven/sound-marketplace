package com.soundmarketplace.controller;

import com.soundmarketplace.service.RateLimiterService;
import com.soundmarketplace.service.UserAccountService;
import com.soundmarketplace.sound_marketplace.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserAccountService userAccountService;
    private final RateLimiterService rateLimiterService;

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private <T> ResponseEntity<ApiResponse<T>> rateLimitedResponse() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60")
                .body(ApiResponse.error("Too many requests. Please try again in 60 seconds."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        if (!rateLimiterService.tryAcquire("login", clientIp, request.getEmail())) {
            return rateLimitedResponse();
        }
        LoginResponse response = userAccountService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = userAccountService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @RequestParam(value = "refreshToken", required = false) String refreshTokenParam) {
        String token = request != null && request.getRefreshToken() != null
                ? request.getRefreshToken()
                : refreshTokenParam;
        userAccountService.logout(token);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserAccountDTO>> register(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest servletRequest) {
        if (request.getClientIp() == null || request.getClientIp().isBlank()) {
            request.setClientIp(extractClientIp(servletRequest));
        }
        UserAccountDTO user = userAccountService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User registered successfully", user));
    }

    @PostMapping(value = "/register-with-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserAccountDTO>> registerWithAvatar(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "termsAccepted", defaultValue = "false") Boolean termsAccepted,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            HttpServletRequest servletRequest) throws IOException {

        byte[] avatarBytes = null;
        if (avatar != null && !avatar.isEmpty()) {
            avatarBytes = avatar.getBytes();
        }

        CreateUserRequest request = CreateUserRequest.builder()
                .email(email)
                .password(password)
                .displayName(displayName)
                .country(country)
                .role(role)
                .termsAccepted(termsAccepted)
                .clientIp(extractClientIp(servletRequest))
                .avatarImageBlob(avatarBytes)
                .build();

        UserAccountDTO user = userAccountService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User registered successfully", user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        if (!rateLimiterService.tryAcquire("forgot-password", clientIp, request.getEmail())) {
            return rateLimitedResponse();
        }
        userAccountService.requestPasswordReset(request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("If an account exists with this email, a reset link has been sent.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userAccountService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully.", null));
    }

    @PostMapping("/recover-username")
    public ResponseEntity<ApiResponse<Void>> recoverUsername(@RequestParam String email) {
        userAccountService.recoverUsername(email);
        return ResponseEntity.ok(ApiResponse.ok("If an account exists with this email, your username has been sent.", null));
    }

    @RequestMapping(value = "/verify-email", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<ApiResponse<UserAccountDTO>> verifyEmail(
            @RequestParam("token") String token,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        if (!rateLimiterService.tryAcquire("verify-email", clientIp, null)) {
            return rateLimitedResponse();
        }
        UserAccountDTO user = userAccountService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok("Email has been verified successfully.", user));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam("email") String email,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        if (!rateLimiterService.tryAcquire("resend-verification", clientIp, email)) {
            return rateLimitedResponse();
        }
        userAccountService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.ok("Verification email has been resent.", null));
    }
}
