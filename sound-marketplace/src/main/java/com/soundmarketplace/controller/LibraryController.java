package com.soundmarketplace.controller;

import com.soundmarketplace.service.OrderService;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.TrackDTO;
import com.soundmarketplace.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TrackDTO>>> getLibrary(Pageable pageable) {
        Integer userId = CurrentUser.getUserId();
        Page<TrackDTO> library = orderService.getLibrary(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(library));
    }
}
