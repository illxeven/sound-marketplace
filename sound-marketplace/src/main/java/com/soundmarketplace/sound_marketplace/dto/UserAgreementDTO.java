package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAgreementDTO {
    private Long id;
    private Long userId;
    private Long documentId;
    private String documentVersion;
    private LocalDateTime agreedAt;
    private String ipAddress;
}
