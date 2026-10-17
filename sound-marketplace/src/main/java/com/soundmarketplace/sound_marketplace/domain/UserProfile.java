package com.soundmarketplace.sound_marketplace.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "country")
    private String country;

    @Lob
    @Column(name = "avatar_image_blob", columnDefinition = "LONGBLOB")
    private byte[] avatarImageBlob;

    public Integer getUserAccountId() {
        return userAccount != null ? userAccount.getId() : null;
    }
}
