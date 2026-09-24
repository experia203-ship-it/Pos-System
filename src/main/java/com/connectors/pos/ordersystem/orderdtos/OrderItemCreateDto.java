package com.connectors.pos.ordersystem.orderdtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record OrderItemCreateDto(
       Long productId,
@NotNull(message = "quantity is required")
@PositiveOrZero(message ="quantity needs to be at least 0")
       int quantity,

     @NotNull(message="sub discount is required , use 0% for no discount")
     @PositiveOrZero(message="discount can't be below 0")
     BigDecimal subDiscount ,
       String barcode,
       String customName,
       BigDecimal customSellingPrice,
       BigDecimal customPurchasePrice




) {
}
