package com.connectors.pos.ordersystem.orderdtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record OrderItemCreateDto(
       Long productId,
@NotNull(message = "{validation.orderItem.quantity.required}")
@Positive(message ="{validation.orderItem.quantity.positive}")
       int quantity,

     @NotNull(message="{validation.orderItem.subDiscount.required}")
     @PositiveOrZero(message="{validation.orderItem.subDiscount.positiveOrZero}")
     BigDecimal subDiscount ,
       String barcode,
       String customName,
       @DecimalMin(value = "0.00", message = "{validation.orderItem.sellingPrice.negative}")
       BigDecimal customSellingPrice,
       @DecimalMin(value = "0.00", message = "{validation.orderItem.purchasePrice.negative}")
       BigDecimal customPurchasePrice




) {
}
