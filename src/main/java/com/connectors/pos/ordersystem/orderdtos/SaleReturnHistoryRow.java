package com.connectors.pos.ordersystem.orderdtos;

import com.connectors.pos.ordersystem.PaymentMethod;
import com.connectors.pos.ordersystem.SaleReturnType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleReturnHistoryRow(
        LocalDateTime createdAt,
        SaleReturnType type,
        String reason,
        String userName,
        String itemName,
        int quantity,
        boolean restocked,
        BigDecimal credit,
        BigDecimal refund,
        PaymentMethod paymentMethod,
        String paymentReference
) {
}
