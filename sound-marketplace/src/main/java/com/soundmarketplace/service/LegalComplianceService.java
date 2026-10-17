package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.dto.LegalDocumentDTO;
import com.soundmarketplace.sound_marketplace.dto.UserAgreementDTO;
import com.soundmarketplace.sound_marketplace.dto.UserConsentRequest;

import java.util.List;

public interface LegalComplianceService {
    List<LegalDocumentDTO> getActiveLegalDocuments();
    List<LegalDocumentDTO> getActiveLegalDocumentsByType(String documentTypeName);
    UserAgreementDTO recordUserConsent(UserConsentRequest request, String clientIp);
    List<UserAgreementDTO> getUserAgreements(Long userId);
    boolean hasUserAgreedToDocument(Long userId, Long documentId);
}
