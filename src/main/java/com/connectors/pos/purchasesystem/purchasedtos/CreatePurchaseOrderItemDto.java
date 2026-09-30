package com.connectors.pos.purchasesystem.purchasedtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreatePurchaseOrderItemDto(
        Long productId,
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity needs to be at least 1")
        int quantity,
        @NotNull(message = "sub discount is required")
        @PositiveOrZero(message = "discount can't be below 0")
        BigDecimal subDiscount,
        String barcode,
        String customName,
        @Positive(message = "selling price must be greater than zero")
        BigDecimal customSellingPrice,
        @DecimalMin(value = "0.00", message = "purchase price cannot be negative")
        BigDecimal customPurchasePrice
) {}