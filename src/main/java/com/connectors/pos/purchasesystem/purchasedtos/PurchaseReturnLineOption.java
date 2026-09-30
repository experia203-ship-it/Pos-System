package com.connectors.pos.purchasesystem.purchasedtos;

import java.math.BigDecimal;

public record PurchaseReturnLineOption(
        Long purchaseItemId,
        String productName,
        int quantityPurchased,
        long quantityAlreadyReturned,
        long quantityReturnable,
        BigDecimal unitCredit
) {
}
