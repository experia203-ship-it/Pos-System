package com.connectors.pos.customersystem.customerdtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerUpdateDto(

        @NotBlank(message="customer name is required")

        String name,
        String location,
        String shippingCompany,
        String phoneNumber
) {
}
