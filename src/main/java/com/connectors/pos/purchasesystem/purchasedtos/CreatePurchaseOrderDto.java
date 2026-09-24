package com.connectors.pos.purchasesystem.purchasedtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public record CreatePurchaseOrderDto(
        @NotNull(message = "discount is required")
        BigDecimal discount,
        Long supplierId,
        @Valid
        List<CreatePurchaseOrderItemDto> itemsList,
        @NotNull(message = "paid is required")
        @PositiveOrZero(message = "payment must be equal to or more than 0")
        BigDecimal paid,
        BigDecimal remaining,
        String orderNumber
) {}