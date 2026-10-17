package com.soundmarketplace.service.impl;

import com.soundmarketplace.exception.NotFoundException;
import com.soundmarketplace.service.CartService;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final TrackRepository trackRepository;

    @Override
    @Transactional
    public CartDTO getCartByUserId(Integer userId) {
        Integer targetUserId = userId != null ? userId : CurrentUser.getUserId();
        CurrentUser.requireOwnerOrAdmin(targetUserId);
        Cart cart = getOrCreateCart(targetUserId);
        return mapToDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO addToCart(AddToCartRequest request) {
        if (request.getMerchandiseId() == null && request.getTrackId() == null) {
            throw new IllegalArgumentException("Must specify either merchandiseId or trackId to add to cart");
        }

        Integer targetUserId = request.getUserId() != null ? request.getUserId() : CurrentUser.getUserId();
        CurrentUser.requireOwnerOrAdmin(targetUserId);
        request.setUserId(targetUserId);

        Cart cart = getOrCreateCart(targetUserId);
        int addQty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;

        if (request.getMerchandiseId() != null) {
            Merchandise merch = merchandiseRepository.findById(request.getMerchandiseId())
                    .orElseThrow(() -> new NotFoundException("Merchandise not found with id: " + request.getMerchandiseId()));

            Optional<CartItem> existing = cartItemRepository.findByCartIdAndMerchandiseId(cart.getId(), merch.getId());
            if (existing.isPresent()) {
                CartItem item = existing.get();
                item.setQuantity(item.getQuantity() + addQty);
                item.setPriceSnapshot(merch.getPrice());
                cartItemRepository.save(item);
            } else {
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .merchandise(merch)
                        .quantity(addQty)
                        .priceSnapshot(merch.getPrice() != null ? merch.getPrice() : BigDecimal.ZERO)
                        .build();
                cartItemRepository.save(newItem);
            }
        } else if (request.getTrackId() != null) {
            Track track = trackRepository.findById(request.getTrackId())
                    .orElseThrow(() -> new NotFoundException("Track not found with id: " + request.getTrackId()));

            Optional<CartItem> existing = cartItemRepository.findByCartIdAndTrackId(cart.getId(), track.getId());
            if (existing.isPresent()) {
                // Tracks are digital licenses, quantity usually 1
                CartItem item = existing.get();
                item.setPriceSnapshot(track.getPrice() != null ? track.getPrice() : BigDecimal.ZERO);
                cartItemRepository.save(item);
            } else {
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .track(track)
                        .quantity(1)
                        .priceSnapshot(track.getPrice() != null ? track.getPrice() : BigDecimal.ZERO)
                        .build();
                cartItemRepository.save(newItem);
            }
        }

        Cart refreshed = cartRepository.findById(cart.getId()).orElse(cart);
        return mapToDTO(refreshed);
    }

    @Override
    @Transactional
    public CartDTO updateCartItemQuantity(Integer userId, Integer cartItemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with id: " + cartItemId));

        CurrentUser.requireOwnerOrAdmin(item.getCart().getUser().getId());

        if (request.getQuantity() <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        Cart refreshed = cartRepository.findById(item.getCart().getId()).orElse(item.getCart());
        return mapToDTO(refreshed);
    }

    @Override
    @Transactional
    public CartDTO removeCartItem(Integer userId, Integer cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found with id: " + cartItemId));

        CurrentUser.requireOwnerOrAdmin(item.getCart().getUser().getId());

        cartItemRepository.delete(item);
        Cart refreshed = cartRepository.findById(item.getCart().getId()).orElse(item.getCart());
        return mapToDTO(refreshed);
    }

    @Override
    @Transactional
    public void clearCart(Integer userId) {
        Integer targetUserId = userId != null ? userId : CurrentUser.getUserId();
        CurrentUser.requireOwnerOrAdmin(targetUserId);
        Cart cart = getOrCreateCart(targetUserId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private Cart getOrCreateCart(Integer userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
            Cart newCart = Cart.builder()
                    .user(user)
                    .items(new ArrayList<>())
                    .build();
            return cartRepository.save(newCart);
        });
    }

    private CartDTO mapToDTO(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemDTO> itemDTOs = items.stream().map(this::mapItemToDTO).collect(Collectors.toList());

        int totalCount = itemDTOs.stream().mapToInt(CartItemDTO::getQuantity).sum();
        BigDecimal subtotal = itemDTOs.stream()
                .map(CartItemDTO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemDTOs)
                .totalItems(totalCount)
                .subtotal(subtotal)
                .build();
    }

    private CartItemDTO mapItemToDTO(CartItem item) {
        CartItemDTO.CartItemDTOBuilder builder = CartItemDTO.builder()
                .id(item.getId())
                .cartId(item.getCart().getId())
                .quantity(item.getQuantity())
                .unitPrice(item.getPriceSnapshot())
                .totalPrice(item.getPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity())))
                .addedAt(item.getAddedAt());

        if (item.getMerchandise() != null) {
            Merchandise m = item.getMerchandise();
            builder.itemType("MERCHANDISE")
                    .merchandiseId(m.getId())
                    .merchandiseTitle(m.getTitle())
                    .merchandiseCategory(m.getCategory() != null ? m.getCategory().getName() : null)
                    .merchandiseImageUrl(m.getFilePath())
                    .merchandiseImageBlob(m.getImageBlob());
        } else if (item.getTrack() != null) {
            Track t = item.getTrack();
            String artistDisplay = t.getArtistName() != null ? t.getArtistName() :
                    (t.getArtist() != null ? t.getArtist().getEmail().split("@")[0] : "Sound Artist");
            builder.itemType("TRACK")
                    .trackId(t.getId())
                    .trackTitle(t.getTitle())
                    .trackArtist(artistDisplay)
                    .trackCoverUrl("/tracks/" + t.getId() + "/cover")
                    .trackCoverBlob(t.getCoverImageBlob());
        }

        return builder.build();
    }
}
