package com.soundmarketplace.controller;

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
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserAccountService userAccountService;

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

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = userAccountService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("If an account exists with this email, a reset token has been sent.",
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
        return ResponseEntity.ok(ApiResponse.ok("If an account exists with this email, your username has been sent.", null));
    }

    @RequestMapping(value = "/verify-email", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<ApiResponse<UserAccountDTO>> verifyEmail(@RequestParam("token") String token) {
        UserAccountDTO user = userAccountService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok("Email has been verified successfully.", user));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam("email") String email) {
        userAccountService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.ok("Verification email has been resent.", null));
    }
}
