package com.connectors.pos.ordersystem.orderdtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.connectors.pos.ordersystem.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record OrderCreateDto(


        @NotNull(message="discount is required ; for no disc set to 0%")
        BigDecimal discount,


        Long customerId,

@NotEmpty(message="at least on item is required")
@Valid
   List<OrderItemCreateDto> itemsList,
        @NotNull(message="please provide a payment , set to 0 for no payment")
        @PositiveOrZero(message="payment must be equal to or more than 0")
        BigDecimal paid,

        BigDecimal remaining ,

        Long number,

        PaymentMethod paymentMethod,

        @Size(max = 100, message = "Payment reference must be 100 characters or fewer.")
        String paymentReference

) {
    public OrderCreateDto(BigDecimal discount, Long customerId, List<OrderItemCreateDto> itemsList,
                          BigDecimal paid, BigDecimal remaining, Long number) {
        this(discount, customerId, itemsList, paid, remaining, number, PaymentMethod.CASH, null);
    }
}
