package com.connectors.pos.ordersystem.orderdtos;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.connectors.pos.ordersystem.PaymentMethod;

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
        String orderNumber,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        PaymentMethod paymentMethod,
        String paymentReference,
        BigDecimal cashReceived,
        BigDecimal cashChange,
        BigDecimal returnCredit,
        BigDecimal refundedTotal,
        boolean voided
) {
    public OrderResponseDto(Long id, String userName, BigDecimal total, BigDecimal discount,
                            BigDecimal paid, BigDecimal remaining, LocalDateTime createdAt,
                            Long userId, Long customerId, String customerName,
                            List<OrderItemResponseDto> items, String orderNumber) {
        this(id, userName, total, discount, paid, remaining, createdAt, userId, customerId,
                customerName, items, orderNumber, BigDecimal.ZERO, BigDecimal.ZERO,
                PaymentMethod.CASH, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, false);
    }
}
