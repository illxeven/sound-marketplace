package com.soundmarketplace.controller;

import com.soundmarketplace.util.CurrentUser;
import com.soundmarketplace.service.MediaExportService;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
@Slf4j
public class MediaAdminController {

    private final MediaExportService mediaExportService;

    @PostMapping("/export-blobs")
    @PreAuthorize("hasAnyRole('admin', 'super_admin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportBlobs() {
        log.info("Starting media blob export triggered by admin user {}", CurrentUser.getUserId());
        Map<String, Object> result = mediaExportService.exportBlobs();
        return ResponseEntity.ok(ApiResponse.ok("Blob export completed successfully", result));
    }
}
