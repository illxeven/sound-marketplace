package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchandiseCategoryDTO {
    private Long id;
    private String name;
    private Long groupTypeId;
    private String groupTypeName;
    private String groupTypeDescription;
}
