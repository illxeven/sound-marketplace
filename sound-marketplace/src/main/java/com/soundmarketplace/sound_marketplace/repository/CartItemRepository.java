package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartId(Integer cartId);
    Optional<CartItem> findByCartIdAndMerchandiseId(Integer cartId, Integer merchandiseId);
    Optional<CartItem> findByCartIdAndTrackId(Integer cartId, Integer trackId);
    void deleteByCartId(Integer cartId);
}
