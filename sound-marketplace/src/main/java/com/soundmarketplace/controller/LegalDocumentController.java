package com.soundmarketplace.controller;

import com.soundmarketplace.service.LegalComplianceService;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.LegalDocumentDTO;
import com.soundmarketplace.sound_marketplace.dto.UserAgreementDTO;
import com.soundmarketplace.sound_marketplace.dto.UserConsentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/legal")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LegalDocumentController {

    private final LegalComplianceService legalComplianceService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<LegalDocumentDTO>>> getActiveLegalDocuments() {
        List<LegalDocumentDTO> docs = legalComplianceService.getActiveLegalDocuments();
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    @GetMapping("/active/{typeName}")
    public ResponseEntity<ApiResponse<List<LegalDocumentDTO>>> getActiveLegalDocumentsByType(@PathVariable String typeName) {
        List<LegalDocumentDTO> docs = legalComplianceService.getActiveLegalDocumentsByType(typeName);
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    @PostMapping("/consent")
    public ResponseEntity<ApiResponse<UserAgreementDTO>> recordUserConsent(
            @Valid @RequestBody UserConsentRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        UserAgreementDTO agreement = legalComplianceService.recordUserConsent(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User consent recorded successfully", agreement));
    }

    @GetMapping("/agreements/{userId}")
    public ResponseEntity<ApiResponse<List<UserAgreementDTO>>> getUserAgreements(@PathVariable Long userId) {
        List<UserAgreementDTO> agreements = legalComplianceService.getUserAgreements(userId);
        return ResponseEntity.ok(ApiResponse.ok(agreements));
    }

    private String extractClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
