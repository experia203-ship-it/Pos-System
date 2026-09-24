package com.connectors.pos.products.productdtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record ProductResponseDto(
        Long id,
        String name,
        String partNumber,
        String description,
        BigDecimal sellingPrice,
        BigDecimal purchasePrice,
        Long stock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long categoryId,
        String barcode,
        Long reorderPoint,
        Map<String,String> customFields


) {
}
