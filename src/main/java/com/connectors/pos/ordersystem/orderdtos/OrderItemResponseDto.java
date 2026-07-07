package com.connectors.pos.ordersystem.orderdtos;

import java.math.BigDecimal;

public record OrderItemResponseDto(
        Long id,
        String productName,
        BigDecimal productSellingPrice,
        BigDecimal productPurchasePrice,
        int quantity,
        BigDecimal subTotal,
        BigDecimal subDiscount,
        Long productId


) {
}
