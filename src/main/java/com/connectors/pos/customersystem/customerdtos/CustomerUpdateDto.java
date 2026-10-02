package com.connectors.pos.customersystem.customerdtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerUpdateDto(

        @NotBlank(message="{validation.customer.name.required}")
        @Size(max = 50)
        String name,
        @Size(max = 1000)
        String location,
        @Size(max = 50)
        String shippingCompany,
        @Size(max = 50)
        String phoneNumber
) {
}
