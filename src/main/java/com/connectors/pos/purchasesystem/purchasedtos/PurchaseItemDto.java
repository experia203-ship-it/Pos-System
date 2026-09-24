package com.connectors.pos.purchasesystem.purchasedtos;

import java.math.BigDecimal;

public record PurchaseItemDto(

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
