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
public class CreatePlaylistRequest {
    private Integer userId;
    private String title;
    private Boolean isPublic;
    private List<Integer> trackIds;
}
