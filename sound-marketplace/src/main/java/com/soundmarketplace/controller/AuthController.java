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
import java.util.Map;

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
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        if (!rateLimiterService.tryAcquire("forgot-password", clientIp, request.getEmail())) {
            return rateLimitedResponse();
        }
        String token = userAccountService.requestPasswordReset(request.getEmail());
        Map<String, String> data = token != null ? Map.of("token", token) : null;
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("If an account exists with this email, a reset link has been sent.", data));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userAccountService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully.", null));
    }

    @GetMapping("/reset-password")
    public ResponseEntity<String> getResetPasswordPage(@RequestParam(name = "token", required = false) String token) {
        String safeToken = token != null ? token : "";
        String html = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>Reset Password - Sound Marketplace</title>"
                + "<style>"
                + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f1016; color: #ffffff; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 20px; box-sizing: border-box; }"
                + ".card { background: #1a1b26; border: 1px solid rgba(255,255,255,0.1); border-radius: 20px; padding: 40px; max-width: 440px; width: 100%; text-align: center; box-shadow: 0 20px 40px rgba(0,0,0,0.4); }"
                + "h1 { font-size: 22px; margin: 0 0 10px; font-weight: 700; }"
                + "p { color: #8f92a1; font-size: 13px; margin: 0 0 20px; }"
                + ".token-badge { background: rgba(0,229,255,0.1); color: #00e5ff; font-weight: 700; font-size: 16px; letter-spacing: 2px; padding: 10px 16px; border-radius: 8px; margin-bottom: 20px; display: inline-block; }"
                + "input { width: 100%; box-sizing: border-box; padding: 12px 16px; border-radius: 10px; border: 1px solid rgba(255,255,255,0.2); background: #12131a; color: #fff; font-size: 14px; margin-bottom: 16px; }"
                + "button { width: 100%; padding: 12px; border-radius: 10px; border: none; background: #00e5ff; color: #000; font-weight: 700; font-size: 14px; cursor: pointer; }"
                + "button:hover { background: #00b4d8; }"
                + "#msg { margin-top: 15px; font-size: 13px; }"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<h1>Reset Your Password</h1>"
                + "<p>Enter a new password for your Sound Marketplace account.</p>"
                + (safeToken.isEmpty() ? "" : "<div class='token-badge'>Token: " + safeToken + "</div>")
                + "<input type='hidden' id='token' value='" + safeToken + "'>"
                + (safeToken.isEmpty() ? "<input type='text' id='manualToken' placeholder='Enter 8-character reset token'>" : "")
                + "<input type='password' id='newPass' placeholder='New Password (min 6 chars)'>"
                + "<input type='password' id='confirmPass' placeholder='Confirm New Password'>"
                + "<button onclick='submitReset()'>Update Password</button>"
                + "<div id='msg'></div>"
                + "</div>"
                + "<script>"
                + "async function submitReset() {"
                + "  const token = document.getElementById('token').value || (document.getElementById('manualToken') ? document.getElementById('manualToken').value.trim() : '');"
                + "  const newPass = document.getElementById('newPass').value;"
                + "  const confirmPass = document.getElementById('confirmPass').value;"
                + "  const msg = document.getElementById('msg');"
                + "  if (!token) { msg.innerText = 'Reset token is required'; msg.style.color = '#ef4444'; return; }"
                + "  if (!newPass || newPass.length < 6) { msg.innerText = 'Password must be at least 6 characters'; msg.style.color = '#ef4444'; return; }"
                + "  if (newPass !== confirmPass) { msg.innerText = 'Passwords do not match'; msg.style.color = '#ef4444'; return; }"
                + "  try {"
                + "    const res = await fetch('/api/auth/reset-password', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ token: token, newPassword: newPass }) });"
                + "    const data = await res.json();"
                + "    if (res.ok && data.success) {"
                + "      msg.innerHTML = '<span style=\"color:#22c55e;font-weight:bold;\">&#10003; Password updated successfully! You can now log into the app.</span>';"
                + "    } else {"
                + "      msg.innerText = data.message || 'Failed to reset password';"
                + "      msg.style.color = '#ef4444';"
                + "    }"
                + "  } catch (e) {"
                + "    msg.innerText = 'Connection error: ' + e.message;"
                + "    msg.style.color = '#ef4444';"
                + "  }"
                + "}"
                + "</script></body></html>";
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping("/recover-username")
    public ResponseEntity<ApiResponse<Void>> recoverUsername(@RequestParam String email) {
        userAccountService.recoverUsername(email);
        return ResponseEntity.ok(ApiResponse.ok("If an account exists with this email, your username has been sent.", null));
    }

    @RequestMapping(value = "/verify-email", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> verifyEmail(
            @RequestParam("token") String token,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        if (!rateLimiterService.tryAcquire("verify-email", clientIp, null)) {
            return rateLimitedResponse();
        }
        UserAccountDTO user = userAccountService.verifyEmail(token);

        String acceptHeader = servletRequest.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            String html = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<title>Email Verified - Sound Marketplace</title>"
                    + "<style>"
                    + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f1016; color: #ffffff; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }"
                    + ".card { background: #1a1b26; border: 1px solid rgba(255,255,255,0.1); border-radius: 20px; padding: 48px; max-width: 440px; text-align: center; box-shadow: 0 20px 40px rgba(0,0,0,0.4); }"
                    + ".icon { width: 64px; height: 64px; background: rgba(0,229,255,0.1); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 24px; font-size: 32px; color: #00e5ff; }"
                    + "h1 { font-size: 24px; margin: 0 0 12px; font-weight: 700; }"
                    + "p { color: #8f92a1; font-size: 14px; line-height: 1.6; margin: 0 0 24px; }"
                    + ".email { color: #00e5ff; font-weight: 600; }"
                    + ".badge { display: inline-block; background: #22c55e; color: #000; font-weight: 700; padding: 8px 16px; border-radius: 20px; font-size: 13px; letter-spacing: 0.5px; }"
                    + "</style></head><body>"
                    + "<div class='card'>"
                    + "<div class='icon'>&#10003;</div>"
                    + "<h1>Email Verified!</h1>"
                    + "<p>Your account <span class='email'>" + user.getEmail() + "</span> is now verified and active.</p>"
                    + "<p>You can return to the Sound Marketplace app to start exploring, creating, and streaming.</p>"
                    + "<div class='badge'>ALL FEATURES UNLOCKED</div>"
                    + "</div></body></html>";
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }

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
