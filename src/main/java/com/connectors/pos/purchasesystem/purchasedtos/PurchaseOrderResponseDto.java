package com.connectors.pos.purchasesystem.purchasedtos;

import com.connectors.pos.ordersystem.orderdtos.OrderItemResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponseDto(



        Long id,
        String userName,
        BigDecimal total,
        BigDecimal discount,
        BigDecimal paid,
        BigDecimal remaining,
        LocalDateTime createdAt,
        Long userId,
        Long vendorId,
        String vendorName,
        List<PurchaseItemDto> items,
        String orderNumber

) {
}
