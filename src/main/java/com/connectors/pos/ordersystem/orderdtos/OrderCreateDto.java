package com.connectors.pos.ordersystem.orderdtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.connectors.pos.ordersystem.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record OrderCreateDto(


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

        Long number,

        PaymentMethod paymentMethod,

        @Size(max = 100, message = "{validation.order.paymentReference.size}")
        String paymentReference

) {
    public OrderCreateDto(BigDecimal discount, Long customerId, List<OrderItemCreateDto> itemsList,
                          BigDecimal paid, BigDecimal remaining, Long number) {
        this(discount, customerId, itemsList, paid, remaining, number, PaymentMethod.CASH, null);
    }
}
