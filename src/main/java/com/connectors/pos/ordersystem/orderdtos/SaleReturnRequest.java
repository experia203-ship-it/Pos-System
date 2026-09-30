package com.connectors.pos.ordersystem.orderdtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SaleReturnRequest(
        @NotNull(message = "Choose a sale item.")
        Long orderItemId,
        @NotNull(message = "Return quantity is required.")
        @Positive(message = "Return quantity must be at least one.")
        Integer quantity,
        boolean restock,
        @NotBlank(message = "A return reason is required.")
        @Size(max = 255, message = "Return reason must be 255 characters or fewer.")
        String reason
) {
}
