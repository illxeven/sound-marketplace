package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccountDTO {
    private Integer id;
    private String email;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String country;
    private Boolean hasAvatarImage;
    private List<String> roles;
    private LocalDateTime createdAt;
}
