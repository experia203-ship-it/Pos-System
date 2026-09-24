package com.connectors.pos.ordersystem.orderdtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerSummary(
        String name,
        LocalDateTime startDt,
        LocalDateTime endDt,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal remaining,
        Page<OrderResponseDto> orders
) {
}
