package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    @Column(name = "verification_expires_at")
    private LocalDateTime verificationExpiresAt;

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_account_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public boolean hasRole(String roleName) {
        if (roles == null || roleName == null) return false;
        String target = roleName.toLowerCase().replace("role_", "").replace("_", "");
        return roles.stream()
                .anyMatch(r -> {
                    if (r.getRoleName() == null) return false;
                    String rClean = r.getRoleName().toLowerCase().replace("role_", "").replace("_", "");
                    return rClean.equals(target);
                });
    }

    public boolean canUploadTracks() {
        // Track Upload Permissions: Only logged-in users with roles ADMIN, SUPER_ADMIN, MUSIC_ARTIST, or PRODUCER
        return hasRole("ADMIN") ||
               hasRole("SUPER_ADMIN") ||
               hasRole("MUSIC_ARTIST") ||
               hasRole("PRODUCER");
    }

    public boolean canUploadMerchandise() {
        // Merchandise Upload Permissions: ONLY users with the "Artist" role are permitted to upload merchandise items
        return hasRole("ARTIST") ||
               hasRole("MUSIC_ARTIST") ||
               hasRole("ADMIN") ||
               hasRole("SUPER_ADMIN");
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}