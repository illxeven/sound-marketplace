package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementId implements Serializable {

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "track_id")
    private Integer trackId;
}
