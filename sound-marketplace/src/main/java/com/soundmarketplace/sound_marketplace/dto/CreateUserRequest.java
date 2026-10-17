package com.soundmarketplace.sound_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please enter a valid email address with a valid domain (e.g. name@example.com)")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
    private String displayName;
    private String country;
    private String role;
    private List<String> roles;
    private String avatarUrl;
    private byte[] avatarImageBlob;
    private String bio;
    private Boolean termsAccepted;
    private String clientIp;
}
