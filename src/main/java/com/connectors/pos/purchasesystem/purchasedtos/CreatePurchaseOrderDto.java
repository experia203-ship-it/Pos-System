package com.connectors.pos.purchasesystem.purchasedtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

public record CreatePurchaseOrderDto(
        @NotNull(message = "{validation.purchase.discount.required}")
        BigDecimal discount,
        Long supplierId,
        @NotEmpty(message = "{validation.purchase.items.required}")
        @Valid
        List<CreatePurchaseOrderItemDto> itemsList,
        @NotNull(message = "{validation.purchase.paid.required}")
        @PositiveOrZero(message = "{validation.order.paid.positiveOrZero}")
        BigDecimal paid,
        BigDecimal remaining,
        String orderNumber
) {}