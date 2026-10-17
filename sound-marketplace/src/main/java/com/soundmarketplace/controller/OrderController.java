package com.soundmarketplace.controller;

import com.soundmarketplace.service.OrderService;
import com.soundmarketplace.sound_marketplace.dto.ApiResponse;
import com.soundmarketplace.sound_marketplace.dto.OrderDTO;
import com.soundmarketplace.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getMyOrders(Pageable pageable) {
        Integer buyerId = CurrentUser.getUserId();
        Page<OrderDTO> orders = orderService.getMyOrders(buyerId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long id) {
        Integer currentUserId = CurrentUser.getUserId();
        OrderDTO order = orderService.getOrderById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }
}
