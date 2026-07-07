package com.connectors.pos.ordersystem.orderdtos;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
        Long id,
        String userName,
        BigDecimal total,
        BigDecimal discount,
        BigDecimal paid,
        BigDecimal remaining,
        LocalDateTime createdAt,
        Long userId,
        Long customerId,
        String customerName,
        List<OrderItemResponseDto> items,
        String orderNumber
) {
}
