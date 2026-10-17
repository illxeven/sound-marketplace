package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "merchandise_type")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchandiseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_name", nullable = false, unique = true)
    private String typeName;
}
