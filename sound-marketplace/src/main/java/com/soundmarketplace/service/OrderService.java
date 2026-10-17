package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.dto.OrderDTO;
import com.soundmarketplace.sound_marketplace.dto.TrackDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    Page<OrderDTO> getMyOrders(Integer buyerId, Pageable pageable);
    OrderDTO getOrderById(Long orderId, Integer currentUserId);
    Page<TrackDTO> getLibrary(Integer userId, Pageable pageable);
    void handleWebhook(String payload, String sigHeader);
}
