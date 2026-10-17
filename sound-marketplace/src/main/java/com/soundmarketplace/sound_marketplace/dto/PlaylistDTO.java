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
public class PlaylistDTO {
    private Integer id;
    private Integer userId;
    private String userEmail;
    private String title;
    private Boolean isPublic;
    private List<TrackDTO> tracks;
    private LocalDateTime createdAt;
}
