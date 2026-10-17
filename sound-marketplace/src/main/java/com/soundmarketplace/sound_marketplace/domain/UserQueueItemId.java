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
public class UserQueueItemId implements Serializable {

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "position")
    private Integer position;
}
