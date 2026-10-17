package com.soundmarketplace.sound_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email format required")
    private String email;
}
