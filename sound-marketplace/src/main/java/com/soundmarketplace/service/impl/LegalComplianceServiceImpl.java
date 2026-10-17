package com.soundmarketplace.service.impl;

import com.soundmarketplace.service.LegalComplianceService;
import com.soundmarketplace.sound_marketplace.domain.DocumentType;
import com.soundmarketplace.sound_marketplace.domain.LegalDocument;
import com.soundmarketplace.sound_marketplace.domain.UserAgreement;
import com.soundmarketplace.sound_marketplace.dto.LegalDocumentDTO;
import com.soundmarketplace.sound_marketplace.dto.UserAgreementDTO;
import com.soundmarketplace.sound_marketplace.dto.UserConsentRequest;
import com.soundmarketplace.sound_marketplace.repository.LegalDocumentRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LegalComplianceServiceImpl implements LegalComplianceService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final com.soundmarketplace.sound_marketplace.repository.UserAccountRepository userAccountRepository;

    @Override
    public List<LegalDocumentDTO> getActiveLegalDocuments() {
        return legalDocumentRepository.findByIsActiveTrue().stream()
                .map(this::mapToDocumentDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LegalDocumentDTO> getActiveLegalDocumentsByType(String documentTypeName) {
        return legalDocumentRepository.findByDocumentTypes_NameIgnoreCaseAndIsActiveTrue(documentTypeName).stream()
                .map(this::mapToDocumentDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserAgreementDTO recordUserConsent(UserConsentRequest request, String clientIp) {
        com.soundmarketplace.util.CurrentUser.requireOwnerOrAdmin(request.getUserId().intValue());
        LegalDocument document = legalDocumentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new com.soundmarketplace.exception.NotFoundException("Legal document not found with id: " + request.getDocumentId()));

        com.soundmarketplace.sound_marketplace.domain.UserAccount user = userAccountRepository.findById(request.getUserId().intValue())
                .orElseThrow(() -> new com.soundmarketplace.exception.NotFoundException("User not found with id: " + request.getUserId()));

        String ip = (request.getIpAddress() != null && !request.getIpAddress().isBlank())
                ? request.getIpAddress()
                : (clientIp != null && !clientIp.isBlank() ? clientIp : "0.0.0.0");

        UserAgreement agreement = UserAgreement.builder()
                .user(user)
                .document(document)
                .ipAddress(ip)
                .agreedAt(LocalDateTime.now())
                .build();

        UserAgreement saved = userAgreementRepository.save(agreement);
        return mapToAgreementDTO(saved);
    }

    @Override
    public List<UserAgreementDTO> getUserAgreements(Long userId) {
        com.soundmarketplace.util.CurrentUser.requireOwnerOrAdmin(userId.intValue());
        return userAgreementRepository.findByUser_Id(userId.intValue()).stream()
                .map(this::mapToAgreementDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasUserAgreedToDocument(Long userId, Long documentId) {
        return userAgreementRepository.existsByUser_IdAndDocument_Id(userId.intValue(), documentId);
    }

    private LegalDocumentDTO mapToDocumentDTO(LegalDocument doc) {
        List<String> types = doc.getDocumentTypes() != null
                ? doc.getDocumentTypes().stream().map(DocumentType::getName).collect(Collectors.toList())
                : List.of();

        return LegalDocumentDTO.builder()
                .id(doc.getId())
                .version(doc.getVersion())
                .content(doc.getContent())
                .isActive(doc.getIsActive())
                .effectiveDate(doc.getEffectiveDate())
                .createdBy(doc.getCreatedBy())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .documentTypes(types)
                .build();
    }

    private UserAgreementDTO mapToAgreementDTO(UserAgreement ua) {
        return UserAgreementDTO.builder()
                .id(ua.getId())
                .userId(ua.getUserId())
                .documentId(ua.getDocument() != null ? ua.getDocument().getId() : null)
                .documentVersion(ua.getDocument() != null ? ua.getDocument().getVersion() : null)
                .agreedAt(ua.getAgreedAt())
                .ipAddress(ua.getIpAddress())
                .build();
    }
}
