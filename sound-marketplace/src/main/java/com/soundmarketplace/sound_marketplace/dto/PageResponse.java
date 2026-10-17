package com.soundmarketplace.sound_marketplace.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public PageResponse(List<T> items, int page, int size, long totalElements) {
        this(
                items,
                page,
                size,
                totalElements,
                size == 0 ? 1 : (int) Math.ceil((double) totalElements / (double) Math.max(1, size)),
                (long) (page + 1) * size < totalElements
        );
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements) {
        return new PageResponse<>(items, page, size, totalElements);
    }
}
