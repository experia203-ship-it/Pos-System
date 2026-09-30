package com.connectors.pos.ordersystem.orderdtos;

import java.math.BigDecimal;

public record SaleReturnLineOption(
        Long orderItemId,
        String productName,
        int quantitySold,
        long quantityReturned,
        long quantityReturnable,
        BigDecimal unitCredit,
        boolean restockable
) {
}
