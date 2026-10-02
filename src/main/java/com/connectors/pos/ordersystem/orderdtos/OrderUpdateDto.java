package com.connectors.pos.ordersystem.orderdtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import com.connectors.pos.ordersystem.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record OrderUpdateDto(


        @NotNull(message="{validation.order.discount.required}")
        BigDecimal discount,


        Long customerId,

        @NotEmpty(message="{validation.order.items.required}")
        @Valid
        List<OrderItemCreateDto> itemsList,
        @NotNull(message="{validation.order.paid.required}")
        @PositiveOrZero(message="{validation.order.paid.positiveOrZero}")
        BigDecimal paid,

        BigDecimal remaining ,

        String orderNumber,

        PaymentMethod paymentMethod,

        @Size(max = 100, message = "{validation.order.paymentReference.size}")
        String paymentReference


) {
    public OrderUpdateDto(BigDecimal discount, Long customerId, List<OrderItemCreateDto> itemsList,
                          BigDecimal paid, BigDecimal remaining, String orderNumber) {
        this(discount, customerId, itemsList, paid, remaining, orderNumber, PaymentMethod.CASH, null);
    }
}
