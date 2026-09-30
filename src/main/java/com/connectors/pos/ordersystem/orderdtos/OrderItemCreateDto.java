package com.connectors.pos.ordersystem.orderdtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record OrderItemCreateDto(
       Long productId,
@NotNull(message = "quantity is required")
@Positive(message ="quantity needs to be at least 1")
       int quantity,

     @NotNull(message="sub discount is required , use 0% for no discount")
     @PositiveOrZero(message="discount can't be below 0")
     BigDecimal subDiscount ,
       String barcode,
       String customName,
       @DecimalMin(value = "0.00", message = "selling price cannot be negative")
       BigDecimal customSellingPrice,
       @DecimalMin(value = "0.00", message = "purchase price cannot be negative")
       BigDecimal customPurchasePrice




) {
}
