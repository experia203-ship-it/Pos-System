package com.connectors.pos.ordersystem.orderdtos;

import java.math.BigDecimal;

public record CartItemView(
Long productId,
String productName,
int quantity,
BigDecimal subDiscount,
BigDecimal sellingPrice,
BigDecimal subTotal,
String barcode,
String customName,
BigDecimal customSellingPrice,
BigDecimal customPurchasePrice


) {
}
