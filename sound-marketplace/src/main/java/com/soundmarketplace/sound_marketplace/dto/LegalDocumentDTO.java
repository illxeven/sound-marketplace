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
public class LegalDocumentDTO {
    private Long id;
    private String version;
    private String content;
    private Boolean isActive;
    private LocalDateTime effectiveDate;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> documentTypes;
}
