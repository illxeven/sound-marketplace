package com.soundmarketplace.controller;

import com.soundmarketplace.service.MarketplaceService;
import com.soundmarketplace.sound_marketplace.domain.MerchandiseType;
import com.soundmarketplace.sound_marketplace.dto.*;
import com.soundmarketplace.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchandise")
@RequiredArgsConstructor
public class MerchandiseController {

    private final MarketplaceService marketplaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<MerchandiseDTO>> createMerchandise(@RequestBody CreateMerchandiseRequest request) {
        if (request.getSellerId() == null) {
            request.setSellerId(CurrentUser.getUserId());
        }
        MerchandiseDTO item = marketplaceService.createMerchandise(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Merchandise created successfully", item));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<com.soundmarketplace.sound_marketplace.dto.PageResponse<com.soundmarketplace.sound_marketplace.dto.MerchandiseSummaryDTO>>> getAllMerchandise(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        org.springframework.data.domain.Pageable pageable = com.soundmarketplace.util.PaginationUtil.createPageable(page, size, sort);
        var items = marketplaceService.getAllMerchandise(pageable);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchandiseDTO>> getMerchandiseById(@PathVariable Integer id) {
        MerchandiseDTO item = marketplaceService.getMerchandiseById(id);
        return ResponseEntity.ok(ApiResponse.ok(item));
    }

    @GetMapping("/{id}/images/{n}")
    public ResponseEntity<byte[]> getMerchandiseImage(
            @PathVariable Integer id,
            @PathVariable int n,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        byte[] imageBytes = marketplaceService.getMerchandiseImage(id, n);
        return com.soundmarketplace.util.ImageResponseUtil.createCacheableImageResponse(imageBytes, ifNoneMatch);
    }

    @GetMapping("/type/{typeId}")
    public ResponseEntity<ApiResponse<List<MerchandiseDTO>>> getMerchandiseByType(@PathVariable Integer typeId) {
        List<MerchandiseDTO> items = marketplaceService.getMerchandiseByType(typeId);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<com.soundmarketplace.sound_marketplace.dto.PageResponse<com.soundmarketplace.sound_marketplace.dto.MerchandiseSummaryDTO>>> getMerchandiseBySeller(
            @PathVariable Integer sellerId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        org.springframework.data.domain.Pageable pageable = com.soundmarketplace.util.PaginationUtil.createPageable(page, size, sort);
        var items = marketplaceService.getMerchandiseBySeller(sellerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<MerchandiseType>>> getAllMerchandiseTypes() {
        List<MerchandiseType> types = marketplaceService.getAllMerchandiseTypes();
        return ResponseEntity.ok(ApiResponse.ok(types));
    }

    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<MerchandiseGroupTypeDTO>>> getAllMerchandiseGroupTypes() {
        List<MerchandiseGroupTypeDTO> groups = marketplaceService.getAllMerchandiseGroupTypes();
        return ResponseEntity.ok(ApiResponse.ok(groups));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<MerchandiseCategoryDTO>>> getAllMerchandiseCategories() {
        List<MerchandiseCategoryDTO> categories = marketplaceService.getAllMerchandiseCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @GetMapping("/categories/group/{groupTypeId}")
    public ResponseEntity<ApiResponse<List<MerchandiseCategoryDTO>>> getCategoriesByGroup(@PathVariable Long groupTypeId) {
        List<MerchandiseCategoryDTO> categories = marketplaceService.getMerchandiseCategoriesByGroup(groupTypeId);
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchandiseDTO>> updateMerchandise(
            @PathVariable Integer id,
            @RequestBody UpdateMerchandiseRequest request) {
        MerchandiseDTO updated = marketplaceService.updateMerchandise(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Merchandise updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMerchandise(@PathVariable Integer id) {
        marketplaceService.deleteMerchandise(id);
        return ResponseEntity.ok(ApiResponse.ok("Merchandise deleted successfully", null));
    }

    @PostMapping(value = "/{id}/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MerchandiseDTO>> updateMerchandiseMultipart(
            @PathVariable Integer id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "stockQuantity", required = false) Integer stockQuantity,
            @RequestParam(value = "options", required = false) String options,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {

        byte[] imageBytes = null;
        if (image != null && !image.isEmpty()) {
            imageBytes = image.getBytes();
        }

        UpdateMerchandiseRequest req = UpdateMerchandiseRequest.builder()
                .title(title)
                .price(price)
                .description(description)
                .stockQuantity(stockQuantity)
                .options(options)
                .categoryId(categoryId)
                .imageBlob(imageBytes)
                .build();

        MerchandiseDTO updated = marketplaceService.updateMerchandise(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Merchandise updated successfully", updated));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<List<MerchandiseReviewDTO>>> getReviews(@PathVariable Integer id) {
        List<MerchandiseReviewDTO> reviews = marketplaceService.getReviewsByMerchandiseId(id);
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<MerchandiseReviewDTO>> addReview(
            @PathVariable Integer id,
            @Valid @RequestBody CreateMerchandiseReviewRequest request) {
        request.setMerchandiseId(id);
        request.setUserId(CurrentUser.getUserId());
        MerchandiseReviewDTO review = marketplaceService.addReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Review submitted successfully", review));
    }

    @PostMapping("/types")
    public ResponseEntity<ApiResponse<MerchandiseType>> createMerchandiseType(@RequestBody Map<String, String> body) {
        String typeName = body.get("typeName");
        MerchandiseType type = marketplaceService.createMerchandiseType(typeName);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Merchandise type created", type));
    }
}
