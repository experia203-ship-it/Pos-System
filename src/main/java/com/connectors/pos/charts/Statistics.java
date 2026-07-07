package com.connectors.pos.charts;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Statistics(

        BigDecimal totalSales,
        BigDecimal totalProfit,
        LocalDateTime reportDate


) {
}
