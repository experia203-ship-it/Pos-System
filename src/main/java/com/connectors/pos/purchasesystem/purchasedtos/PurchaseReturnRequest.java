package com.connectors.pos.purchasesystem.purchasedtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PurchaseReturnRequest(
        @NotNull(message = "{validation.purchaseReturn.item.required}")
        Long purchaseItemId,
        @NotNull(message = "{validation.common.quantity.required}")
        @Positive(message = "{validation.common.quantity.positive}")
        Integer quantity,
        @NotBlank(message = "{validation.common.reason.required}")
        @Size(max = 255, message = "{validation.common.reason.size}")
        String reason
) {
}
