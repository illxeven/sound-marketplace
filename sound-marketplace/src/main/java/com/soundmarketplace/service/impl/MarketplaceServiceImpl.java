package com.soundmarketplace.service.impl;

import com.soundmarketplace.exception.NotFoundException;
import com.soundmarketplace.service.MarketplaceService;
import com.soundmarketplace.sound_marketplace.domain.*;
import com.soundmarketplace.sound_marketplace.dto.*;
import com.soundmarketplace.sound_marketplace.repository.*;
import com.soundmarketplace.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MarketplaceServiceImpl implements MarketplaceService {

    private final MerchandiseRepository merchandiseRepository;
    private final MerchandiseReviewRepository merchandiseReviewRepository;
    private final MerchandiseTypeRepository merchandiseTypeRepository;
    private final MerchandiseCategoryRepository merchandiseCategoryRepository;
    private final MerchandiseGroupTypeRepository merchandiseGroupTypeRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final com.soundmarketplace.storage.StorageService storageService;

    @Override
    @Transactional
    public MerchandiseDTO createMerchandise(CreateMerchandiseRequest request) {
        if (request.getSellerId() == null) {
            throw new IllegalArgumentException("Seller ID is required to create merchandise");
        }

        UserAccount seller = userAccountRepository.findById(request.getSellerId())
                .orElseThrow(() -> new IllegalArgumentException("Seller not found with id: " + request.getSellerId()));

        // Security check: ONLY users with the "Artist" role are permitted to upload merchandise items
        if (!seller.canUploadMerchandise()) {
            throw new SecurityException("Access Denied: Only users with the 'Artist' role are permitted to upload merchandise items.");
        }

        // Category validation from hierarchical structure
        MerchandiseCategory category = null;
        if (request.getCategoryId() != null) {
            category = merchandiseCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Merchandise category not found with id: " + request.getCategoryId()));
        } else if (request.getMerchandiseTypeId() != null) {
            // Fallback for legacy requests: map merchandiseTypeId to category id if it exists
            category = merchandiseCategoryRepository.findById(request.getMerchandiseTypeId().longValue()).orElse(null);
        }

        if (category == null) {
            // If neither resolved, require category selection
            throw new IllegalArgumentException("A valid merchandise category must be selected from the category structure.");
        }

        MerchandiseType type = null;
        if (request.getMerchandiseTypeId() != null) {
            type = merchandiseTypeRepository.findById(request.getMerchandiseTypeId()).orElse(null);
        }

        int stock = (request.getStockQuantity() != null && request.getStockQuantity() >= 0)
                ? request.getStockQuantity()
                : (request.getQuantity() != null && request.getQuantity() >= 0 ? request.getQuantity() : 10);

        Merchandise merchandise = Merchandise.builder()
                .seller(seller)
                .category(category)
                .merchandiseType(type)
                .title(request.getTitle())
                .price(request.getPrice())
                .description(request.getDescription())
                .imageBlob(request.getImageBlob())
                .imageBlob2(request.getImageBlob2())
                .imageBlob3(request.getImageBlob3())
                .imageBlob4(request.getImageBlob4())
                .imageBlob5(request.getImageBlob5())
                .options(request.getOptions())
                .stockQuantity(stock)
                .quantity(stock)
                .build();

        Merchandise saved = merchandiseRepository.save(merchandise);
        return mapMerchandiseToDTO(saved);
    }

    @Override
    public MerchandiseDTO getMerchandiseById(Integer id) {
        Merchandise item = merchandiseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Merchandise not found with id: " + id));
        return mapMerchandiseToDTO(item);
    }

    @Override
    public PageResponse<MerchandiseSummaryDTO> getAllMerchandise(org.springframework.data.domain.Pageable pageable) {
        return PageResponse.from(merchandiseRepository.findMerchandiseSummaries(pageable));
    }

    @Override
    public List<MerchandiseDTO> getAllMerchandise() {
        return merchandiseRepository.findAll().stream()
                .map(this::mapMerchandiseToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MerchandiseDTO> getMerchandiseByType(Integer typeId) {
        return merchandiseRepository.findByMerchandiseTypeId(typeId).stream()
                .map(this::mapMerchandiseToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<MerchandiseSummaryDTO> getMerchandiseBySeller(Integer sellerId, org.springframework.data.domain.Pageable pageable) {
        return PageResponse.from(merchandiseRepository.findMerchandiseSummariesBySellerId(sellerId, pageable));
    }

    @Override
    public List<MerchandiseDTO> getMerchandiseBySeller(Integer sellerId) {
        return merchandiseRepository.findBySellerId(sellerId).stream()
                .map(this::mapMerchandiseToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] getMerchandiseImage(Integer id, int imageIndex) {
        byte[] blob = switch (imageIndex) {
            case 1 -> merchandiseRepository.findImageBlob1ById(id);
            case 2 -> merchandiseRepository.findImageBlob2ById(id);
            case 3 -> merchandiseRepository.findImageBlob3ById(id);
            case 4 -> merchandiseRepository.findImageBlob4ById(id);
            case 5 -> merchandiseRepository.findImageBlob5ById(id);
            default -> null;
        };
        if (blob != null && blob.length > 0) {
            return blob;
        }

        Merchandise m = merchandiseRepository.findById(id).orElse(null);
        if (m != null) {
            String key = switch (imageIndex) {
                case 1 -> m.getImageKey1();
                case 2 -> m.getImageKey2();
                case 3 -> m.getImageKey3();
                case 4 -> m.getImageKey4();
                case 5 -> m.getImageKey5();
                default -> null;
            };
            if (key != null && !key.isBlank()) {
                return storageService.loadBytes(key);
            }
        }
        return null;
    }

    @Override
    @Transactional
    public MerchandiseType createMerchandiseType(String typeName) {
        return merchandiseTypeRepository.findByTypeNameIgnoreCase(typeName)
                .orElseGet(() -> merchandiseTypeRepository.save(
                        MerchandiseType.builder().typeName(typeName).build()
                ));
    }

    @Override
    public List<MerchandiseType> getAllMerchandiseTypes() {
        return merchandiseTypeRepository.findAll();
    }

    @Override
    public List<MerchandiseGroupTypeDTO> getAllMerchandiseGroupTypes() {
        return merchandiseGroupTypeRepository.findAll().stream()
                .map(this::mapGroupTypeToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MerchandiseCategoryDTO> getAllMerchandiseCategories() {
        return merchandiseCategoryRepository.findAll().stream()
                .map(this::mapCategoryToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MerchandiseCategoryDTO> getMerchandiseCategoriesByGroup(Long groupTypeId) {
        return merchandiseCategoryRepository.findByGroupTypeId(groupTypeId).stream()
                .map(this::mapCategoryToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentHistoryDTO processPayment(CreatePaymentRequest request) {
        UserAccount buyer = userAccountRepository.findById(request.getBuyerId())
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found with id: " + request.getBuyerId()));

        Merchandise merchandise = null;
        if (request.getMerchandiseId() != null) {
            merchandise = merchandiseRepository.findById(request.getMerchandiseId())
                    .orElseThrow(() -> new IllegalArgumentException("Merchandise not found with id: " + request.getMerchandiseId()));
            
            // Auto-decrease inventory stock quantity upon purchase
            int currentStock = merchandise.getStockQuantity() != null ? merchandise.getStockQuantity() :
                    (merchandise.getQuantity() != null ? merchandise.getQuantity() : 0);
            int newStock = Math.max(0, currentStock - 1);
            merchandise.setStockQuantity(newStock);
            merchandise.setQuantity(newStock);
            merchandiseRepository.save(merchandise);
        }

        String chargeId = (request.getStripeChargeId() != null && !request.getStripeChargeId().isBlank())
                ? request.getStripeChargeId()
                : "ch_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        PaymentHistory payment = PaymentHistory.builder()
                .buyer(buyer)
                .merchandise(merchandise)
                .stripeChargeId(chargeId)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status(request.getStatus() != null ? request.getStatus() : "COMPLETED")
                .build();

        PaymentHistory saved = paymentHistoryRepository.save(payment);
        return mapPaymentToDTO(saved);
    }

    @Override
    public List<PaymentHistoryDTO> getPaymentHistoryByBuyer(Integer buyerId) {
        return paymentHistoryRepository.findByBuyerId(buyerId).stream()
                .map(this::mapPaymentToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketplaceItemDTO> getMarketplaceItems(String category, String genre, BigDecimal minPrice, BigDecimal maxPrice, String search) {
        List<MarketplaceItemDTO> items = new ArrayList<>();
        String cat = (category != null) ? category.toLowerCase().trim() : "all";

        // 1. Fetch Tracks (Songs & Beats)
        if (cat.equals("all") || cat.equals("songs") || cat.equals("tracks") || cat.equals("beats")) {
            List<Track> tracks = trackRepository.findAll();
            for (Track t : tracks) {
                boolean isBeat = (t.getGenre() != null && t.getGenre().getName() != null && t.getGenre().getName().toLowerCase().contains("beat"))
                        || (t.getTitle() != null && t.getTitle().toLowerCase().contains("beat"));
                if (cat.equals("beats") && !isBeat) continue;
                if ((cat.equals("songs") || cat.equals("tracks")) && isBeat) continue;

                String itemType = isBeat ? "BEAT" : "SONG";
                String coverUrl = t.getCoverImageBlob() != null ? "/api/tracks/" + t.getId() + "/cover" :
                        (t.getAlbum() != null ? t.getAlbum().getCoverArtUrl() : null);
                items.add(MarketplaceItemDTO.builder()
                        .id(t.getId())
                        .itemType(itemType)
                        .title(t.getTitle())
                        .description(t.getFeatures() != null ? "Featuring: " + t.getFeatures() : "Audio track")
                        .price(t.getPrice() != null ? t.getPrice() : BigDecimal.ZERO)
                        .currency("USD")
                        .sellerId(t.getArtist() != null ? t.getArtist().getId() : null)
                        .sellerName(t.getArtistName())
                        .genre(t.getGenre() != null ? t.getGenre().getName() : null)
                        .coverArtUrl(coverUrl)
                        .createdAt(t.getCreatedAt())
                        .build());
            }
        }

        // 2. Fetch Albums
        if (cat.equals("all") || cat.equals("albums")) {
            List<Album> albums = albumRepository.findAll();
            for (Album a : albums) {
                items.add(MarketplaceItemDTO.builder()
                        .id(a.getId())
                        .itemType("ALBUM")
                        .title(a.getTitle())
                        .description("Full Album Release")
                        .price(BigDecimal.valueOf(9.99)) // Standard album price
                        .currency("USD")
                        .sellerId(a.getArtist() != null ? a.getArtist().getId() : null)
                        .sellerName(a.getArtist() != null ? a.getArtist().getEmail().split("@")[0] : null)
                        .genre("Album")
                        .coverArtUrl(a.getCoverArtUrl())
                        .createdAt(a.getCreatedAt())
                        .build());
            }
        }

        // 3. Fetch Merchandise
        if (cat.equals("all") || cat.equals("merch") || cat.equals("merchandise")) {
            List<Merchandise> merchList = merchandiseRepository.findAll();
            for (Merchandise m : merchList) {
                String coverUrl = m.getImageBlob() != null ? "/api/merchandise/" + m.getId() + "/images/1" : null;
                items.add(MarketplaceItemDTO.builder()
                        .id(m.getId())
                        .itemType("MERCH")
                        .title(m.getTitle())
                        .description(m.getDescription() != null ? m.getDescription() : (m.getMerchandiseType() != null ? m.getMerchandiseType().getTypeName() : "Merchandise"))
                        .price(m.getPrice())
                        .currency("USD")
                        .sellerId(m.getSeller() != null ? m.getSeller().getId() : null)
                        .sellerName(m.getSeller() != null ? m.getSeller().getEmail().split("@")[0] : null)
                        .genre(m.getMerchandiseType() != null ? m.getMerchandiseType().getTypeName() : null)
                        .coverArtUrl(coverUrl)
                        .options(m.getOptions())
                        .createdAt(m.getCreatedAt())
                        .build());
            }
        }

        // Apply In-Memory Filters
        return items.stream()
                .filter(item -> {
                    if (genre != null && !genre.isBlank()) {
                        if (item.getGenre() == null || !item.getGenre().equalsIgnoreCase(genre.trim())) {
                            return false;
                        }
                    }
                    if (minPrice != null && item.getPrice() != null) {
                        if (item.getPrice().compareTo(minPrice) < 0) return false;
                    }
                    if (maxPrice != null && item.getPrice() != null) {
                        if (item.getPrice().compareTo(maxPrice) > 0) return false;
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean matchTitle = item.getTitle() != null && item.getTitle().toLowerCase().contains(s);
                        boolean matchSeller = item.getSellerName() != null && item.getSellerName().toLowerCase().contains(s);
                        boolean matchDesc = item.getDescription() != null && item.getDescription().toLowerCase().contains(s);
                        if (!matchTitle && !matchSeller && !matchDesc) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentHistoryDTO buyTrack(Integer buyerId, Integer trackId, String stripeChargeId) {
        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found with id: " + trackId));

        CreatePaymentRequest req = CreatePaymentRequest.builder()
                .buyerId(buyerId)
                .trackId(trackId)
                .itemType("TRACK")
                .amount(track.getPrice() != null ? track.getPrice() : BigDecimal.valueOf(0.99))
                .stripeChargeId(stripeChargeId != null ? stripeChargeId : "ch_trk_" + UUID.randomUUID().toString().substring(0, 8))
                .currency("USD")
                .status("COMPLETED")
                .build();

        return processPayment(req);
    }

    @Override
    @Transactional
    public PaymentHistoryDTO buyMerchandiseItem(Integer buyerId, Integer merchandiseId, String stripeChargeId) {
        Merchandise merch = merchandiseRepository.findById(merchandiseId)
                .orElseThrow(() -> new IllegalArgumentException("Merchandise not found with id: " + merchandiseId));

        CreatePaymentRequest req = CreatePaymentRequest.builder()
                .buyerId(buyerId)
                .merchandiseId(merchandiseId)
                .itemType("MERCHANDISE")
                .amount(merch.getPrice())
                .stripeChargeId(stripeChargeId != null ? stripeChargeId : "ch_merch_" + UUID.randomUUID().toString().substring(0, 8))
                .currency("USD")
                .status("COMPLETED")
                .build();

        return processPayment(req);
    }

    @Override
    @Transactional
    public PaymentHistoryDTO buyAlbum(Integer buyerId, Integer albumId, String stripeChargeId) {
        albumRepository.findById(albumId)
                .orElseThrow(() -> new IllegalArgumentException("Album not found with id: " + albumId));

        CreatePaymentRequest req = CreatePaymentRequest.builder()
                .buyerId(buyerId)
                .albumId(albumId)
                .itemType("ALBUM")
                .amount(BigDecimal.valueOf(9.99))
                .stripeChargeId(stripeChargeId != null ? stripeChargeId : "ch_alb_" + UUID.randomUUID().toString().substring(0, 8))
                .currency("USD")
                .status("COMPLETED")
                .build();

        return processPayment(req);
    }

    @Override
    @Transactional
    public MerchandiseDTO updateMerchandise(Integer id, UpdateMerchandiseRequest request) {
        Merchandise item = merchandiseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Merchandise not found with id: " + id));
        CurrentUser.requireOwnerOrAdmin(item.getSeller() != null ? item.getSeller().getId() : null);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription().trim());
        }
        if (request.getStockQuantity() != null) {
            item.setStockQuantity(Math.max(0, request.getStockQuantity()));
            item.setQuantity(Math.max(0, request.getStockQuantity()));
        } else if (request.getQuantity() != null) {
            item.setStockQuantity(Math.max(0, request.getQuantity()));
            item.setQuantity(Math.max(0, request.getQuantity()));
        }
        if (request.getOptions() != null) {
            item.setOptions(request.getOptions().trim());
        }
        if (request.getImageBlob() != null && request.getImageBlob().length > 0) {
            item.setImageBlob(request.getImageBlob());
        }
        if (request.getImageBlob2() != null && request.getImageBlob2().length > 0) {
            item.setImageBlob2(request.getImageBlob2());
        }
        if (request.getImageBlob3() != null && request.getImageBlob3().length > 0) {
            item.setImageBlob3(request.getImageBlob3());
        }
        if (request.getImageBlob4() != null && request.getImageBlob4().length > 0) {
            item.setImageBlob4(request.getImageBlob4());
        }
        if (request.getImageBlob5() != null && request.getImageBlob5().length > 0) {
            item.setImageBlob5(request.getImageBlob5());
        }
        if (request.getCategoryId() != null) {
            merchandiseCategoryRepository.findById(request.getCategoryId()).ifPresent(item::setCategory);
        }
        if (request.getMerchandiseTypeId() != null) {
            merchandiseTypeRepository.findById(request.getMerchandiseTypeId()).ifPresent(item::setMerchandiseType);
        }

        Merchandise saved = merchandiseRepository.save(item);
        return mapMerchandiseToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteMerchandise(Integer id) {
        Merchandise item = merchandiseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Merchandise not found with id: " + id));
        CurrentUser.requireOwnerOrAdmin(item.getSeller() != null ? item.getSeller().getId() : null);
        item.setDeletedAt(java.time.LocalDateTime.now());
        merchandiseRepository.save(item);
    }

    @Override
    @Transactional
    public MerchandiseReviewDTO addReview(CreateMerchandiseReviewRequest request) {
        Merchandise merch = merchandiseRepository.findById(request.getMerchandiseId())
                .orElseThrow(() -> new IllegalArgumentException("Merchandise not found with id: " + request.getMerchandiseId()));
        UserAccount user = userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));

        int rating = Math.min(5, Math.max(1, request.getRating()));

        MerchandiseReview review = MerchandiseReview.builder()
                .merchandise(merch)
                .user(user)
                .rating(rating)
                .comment(request.getComment() != null ? request.getComment().trim() : null)
                .build();

        MerchandiseReview saved = merchandiseReviewRepository.save(review);
        return mapReviewToDTO(saved);
    }

    @Override
    public List<MerchandiseReviewDTO> getReviewsByMerchandiseId(Integer merchandiseId) {
        return merchandiseReviewRepository.findByMerchandiseIdOrderByCreatedAtDesc(merchandiseId)
                .stream()
                .map(this::mapReviewToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void decreaseStock(Integer merchandiseId, int quantity) {
        merchandiseRepository.findById(merchandiseId).ifPresent(m -> {
            int current = m.getStockQuantity() != null ? m.getStockQuantity() :
                    (m.getQuantity() != null ? m.getQuantity() : 0);
            int newQty = Math.max(0, current - quantity);
            m.setStockQuantity(newQty);
            m.setQuantity(newQty);
            merchandiseRepository.save(m);
        });
    }

    private MerchandiseReviewDTO mapReviewToDTO(MerchandiseReview review) {
        String displayName = "Music Fan";
        if (review.getUser() != null) {
            displayName = review.getUser().getEmail().split("@")[0];
        }
        return MerchandiseReviewDTO.builder()
                .id(review.getId())
                .merchandiseId(review.getMerchandise() != null ? review.getMerchandise().getId() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userDisplayName(displayName)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private MerchandiseDTO mapMerchandiseToDTO(Merchandise item) {
        Double avgRating = merchandiseReviewRepository.getAverageRatingByMerchandiseId(item.getId());
        long revCount = merchandiseReviewRepository.countByMerchandiseId(item.getId());
        int finalStock = item.getStockQuantity() != null ? item.getStockQuantity() :
                (item.getQuantity() != null ? item.getQuantity() : 0);

        int imageCount = 0;
        if (item.getImageBlob5() != null && item.getImageBlob5().length > 0) imageCount = 5;
        else if (item.getImageBlob4() != null && item.getImageBlob4().length > 0) imageCount = 4;
        else if (item.getImageBlob3() != null && item.getImageBlob3().length > 0) imageCount = 3;
        else if (item.getImageBlob2() != null && item.getImageBlob2().length > 0) imageCount = 2;
        else if (item.getImageBlob() != null && item.getImageBlob().length > 0) imageCount = 1;

        String coverImageUrl = (imageCount > 0) ? "/api/merchandise/" + item.getId() + "/images/1" : null;

        return MerchandiseDTO.builder()
                .id(item.getId())
                .merchandiseTypeId(item.getMerchandiseType() != null ? item.getMerchandiseType().getId() : null)
                .merchandiseTypeName(item.getMerchandiseType() != null ? item.getMerchandiseType().getTypeName() : null)
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .groupTypeId(item.getCategory() != null && item.getCategory().getGroupType() != null ? item.getCategory().getGroupType().getId() : null)
                .groupTypeName(item.getCategory() != null && item.getCategory().getGroupType() != null ? item.getCategory().getGroupType().getName() : null)
                .sellerId(item.getSeller() != null ? item.getSeller().getId() : null)
                .title(item.getTitle())
                .price(item.getPrice())
                .description(item.getDescription())
                .coverImageUrl(coverImageUrl)
                .imageCount(imageCount)
                .options(item.getOptions())
                .stockQuantity(finalStock)
                .quantity(finalStock)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : null)
                .reviewCount(revCount)
                .createdAt(item.getCreatedAt())
                .build();
    }

    private MerchandiseCategoryDTO mapCategoryToDTO(MerchandiseCategory cat) {
        return MerchandiseCategoryDTO.builder()
                .id(cat.getId())
                .name(cat.getName())
                .groupTypeId(cat.getGroupType() != null ? cat.getGroupType().getId() : null)
                .groupTypeName(cat.getGroupType() != null ? cat.getGroupType().getName() : null)
                .groupTypeDescription(cat.getGroupType() != null ? cat.getGroupType().getDescription() : null)
                .build();
    }

    private MerchandiseGroupTypeDTO mapGroupTypeToDTO(MerchandiseGroupType group) {
        List<MerchandiseCategoryDTO> cats = group.getCategories() != null
                ? group.getCategories().stream().map(this::mapCategoryToDTO).collect(Collectors.toList())
                : List.of();

        return MerchandiseGroupTypeDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .categories(cats)
                .build();
    }

    private PaymentHistoryDTO mapPaymentToDTO(PaymentHistory payment) {
        return PaymentHistoryDTO.builder()
                .id(payment.getId())
                .buyerId(payment.getBuyer() != null ? payment.getBuyer().getId() : null)
                .buyerEmail(payment.getBuyer() != null ? payment.getBuyer().getEmail() : null)
                .merchandiseId(payment.getMerchandise() != null ? payment.getMerchandise().getId() : null)
                .merchandiseTitle(payment.getMerchandise() != null ? payment.getMerchandise().getTitle() : null)
                .stripeChargeId(payment.getStripeChargeId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
