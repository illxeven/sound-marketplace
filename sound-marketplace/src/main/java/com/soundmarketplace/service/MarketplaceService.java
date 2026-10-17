package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.domain.MerchandiseType;
import com.soundmarketplace.sound_marketplace.dto.*;

import java.math.BigDecimal;
import java.util.List;

public interface MarketplaceService {
    MerchandiseDTO createMerchandise(CreateMerchandiseRequest request);
    MerchandiseDTO getMerchandiseById(Integer id);
    PageResponse<MerchandiseSummaryDTO> getAllMerchandise(org.springframework.data.domain.Pageable pageable);
    List<MerchandiseDTO> getAllMerchandise();
    List<MerchandiseDTO> getMerchandiseByType(Integer typeId);
    PageResponse<MerchandiseSummaryDTO> getMerchandiseBySeller(Integer sellerId, org.springframework.data.domain.Pageable pageable);
    List<MerchandiseDTO> getMerchandiseBySeller(Integer sellerId);
    byte[] getMerchandiseImage(Integer id, int imageIndex);

    MerchandiseType createMerchandiseType(String typeName);
    List<MerchandiseType> getAllMerchandiseTypes();

    List<MerchandiseGroupTypeDTO> getAllMerchandiseGroupTypes();
    List<MerchandiseCategoryDTO> getAllMerchandiseCategories();
    List<MerchandiseCategoryDTO> getMerchandiseCategoriesByGroup(Long groupTypeId);

    MerchandiseDTO updateMerchandise(Integer id, UpdateMerchandiseRequest request);
    void deleteMerchandise(Integer id);
    MerchandiseReviewDTO addReview(CreateMerchandiseReviewRequest request);
    List<MerchandiseReviewDTO> getReviewsByMerchandiseId(Integer merchandiseId);
    void decreaseStock(Integer merchandiseId, int quantity);

    PaymentHistoryDTO processPayment(CreatePaymentRequest request);
    List<PaymentHistoryDTO> getPaymentHistoryByBuyer(Integer buyerId);

    // Multi-category marketplace filter & purchasing
    List<MarketplaceItemDTO> getMarketplaceItems(String category, String genre, BigDecimal minPrice, BigDecimal maxPrice, String search);
    PaymentHistoryDTO buyTrack(Integer buyerId, Integer trackId, String stripeChargeId);
    PaymentHistoryDTO buyMerchandiseItem(Integer buyerId, Integer merchandiseId, String stripeChargeId);
    PaymentHistoryDTO buyAlbum(Integer buyerId, Integer albumId, String stripeChargeId);
}
