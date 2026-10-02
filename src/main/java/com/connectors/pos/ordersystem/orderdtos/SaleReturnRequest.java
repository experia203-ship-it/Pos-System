package com.connectors.pos.ordersystem.orderdtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SaleReturnRequest(
        @NotNull(message = "{validation.saleReturn.item.required}")
        Long orderItemId,
        @NotNull(message = "{validation.common.quantity.required}")
        @Positive(message = "{validation.common.quantity.positive}")
        Integer quantity,
        boolean restock,
        @NotBlank(message = "{validation.common.reason.required}")
        @Size(max = 255, message = "{validation.common.reason.size}")
        String reason
) {
}
