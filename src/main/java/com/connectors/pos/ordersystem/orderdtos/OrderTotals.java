package com.connectors.pos.ordersystem.orderdtos;

import java.math.BigDecimal;

public record OrderTotals(
        BigDecimal orderTotal,
        BigDecimal paidTotal,
        BigDecimal remainingTotal

) {
}
