package com.soundmarketplace.controller;

import com.soundmarketplace.service.MarketplaceService;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.MarketplaceItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<MarketplaceItemDTO>>> getMarketplaceItems(
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search) {

        List<MarketplaceItemDTO> items = marketplaceService.getMarketplaceItems(category, genre, minPrice, maxPrice, search);
        return ResponseEntity.ok(ApiResponse.ok("Marketplace items retrieved successfully", items));
    }
}
