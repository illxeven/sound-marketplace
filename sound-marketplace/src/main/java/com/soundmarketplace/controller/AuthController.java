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
import org.springframework.web.util.HtmlUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    /** Same message for known and unknown emails, so this endpoint can't reveal who has an account. */
    static final String FORGOT_PASSWORD_MESSAGE =
            "If an account exists for this email, we've sent a reset code. Check your inbox and spam folder.";

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
        userAccountService.requestPasswordReset(request.getEmail(), clientIp);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(FORGOT_PASSWORD_MESSAGE, null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userAccountService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully.", null));
    }

    @GetMapping("/reset-password")
    public ResponseEntity<String> getResetPasswordPage(@RequestParam(name = "token", required = false) String token) {
        // Escape: the token comes from the URL and is written into HTML and an attribute.
        String safeToken = token != null ? HtmlUtils.htmlEscape(token.trim()) : "";
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
                + (safeToken.isEmpty() ? "<input type='text' id='manualToken' placeholder='Enter the 8-character reset code'>" : "")
                + "<input type='password' id='newPass' placeholder='New Password (min 10 chars)'>"
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
                + "  if (!newPass || newPass.length < 10) { msg.innerText = 'Password must be at least 10 characters'; msg.style.color = '#ef4444'; return; }"
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

    /**
     * Activates an account from the emailed link or code. {@code /verify} is the short link used in
     * new emails; {@code /verify-email} stays for links already sent.
     */
    @RequestMapping(value = {"/verify", "/verify-email"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> verifyEmail(
            @RequestParam("token") String token,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        if (!rateLimiterService.tryAcquire("verify-email", clientIp, null)) {
            return rateLimitedResponse();
        }
        String acceptHeader = servletRequest.getHeader("Accept");
        boolean wantsHtml = acceptHeader != null && acceptHeader.contains("text/html");

        UserAccountDTO user;
        try {
            user = userAccountService.verifyEmail(token);
        } catch (IllegalArgumentException e) {
            if (!wantsHtml) {
                throw e;   // JSON clients get the normal 400 from GlobalExceptionHandler
            }
            log.info("EVENT auth.verify.page_error reason={}", e.getMessage());
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML).body(verifyErrorPage(e.getMessage()));
        }

        if (wantsHtml) {
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
                    + "<p>Your account <span class='email'>" + HtmlUtils.htmlEscape(user.getEmail()) + "</span> is now verified and active.</p>"
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
        // Generic: doesn't reveal whether the email exists or is already verified.
        return ResponseEntity.ok(ApiResponse.ok("If this account still needs verifying, a new code is on its way.", null));
    }

    private static String verifyErrorPage(String message) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>Verification failed - Sound Marketplace</title>"
                + "<style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#0f1016;color:#fff;"
                + "display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;padding:20px;box-sizing:border-box}"
                + ".card{background:#1a1b26;border:1px solid rgba(255,255,255,0.1);border-radius:20px;padding:40px;max-width:440px;text-align:center}"
                + "h1{font-size:22px;margin:0 0 12px}p{color:#ef4444;font-size:14px;line-height:1.6;margin:0 0 12px}"
                + ".hint{color:#8f92a1}</style></head><body><div class='card'>"
                + "<h1>We couldn't verify your email</h1>"
                + "<p>" + HtmlUtils.htmlEscape(message) + "</p>"
                + "<p class='hint'>Open the Sound Marketplace app and tap <b>Resend code</b> on the sign-in screen.</p>"
                + "</div></body></html>";
    }
}
