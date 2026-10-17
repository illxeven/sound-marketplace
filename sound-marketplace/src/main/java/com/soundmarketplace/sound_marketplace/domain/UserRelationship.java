package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_relationships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private UserAccount follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followed_id", nullable = false)
    private UserAccount followed;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACCEPTED";
}
