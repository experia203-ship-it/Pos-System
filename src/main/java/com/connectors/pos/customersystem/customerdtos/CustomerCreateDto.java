package com.connectors.pos.customersystem.customerdtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record CustomerCreateDto(
        @NotBlank(message="customer name is required")
       @Size(max=50)
        String name,
         String location,
        String shippingCompany


) {
}
