package com.connectors.pos.purchasesystem.purchasedtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreatePurchaseOrderItemDto(
        Long productId,
        @NotNull(message = "{validation.orderItem.quantity.required}")
        @Positive(message = "{validation.orderItem.quantity.positive}")
        int quantity,
        @NotNull(message = "{validation.purchaseItem.subDiscount.required}")
        @PositiveOrZero(message = "{validation.orderItem.subDiscount.positiveOrZero}")
        BigDecimal subDiscount,
        String barcode,
        String customName,
        @Positive(message = "{validation.purchaseItem.sellingPrice.positive}")
        BigDecimal customSellingPrice,
        @DecimalMin(value = "0.00", message = "{validation.orderItem.purchasePrice.negative}")
        BigDecimal customPurchasePrice
) {}