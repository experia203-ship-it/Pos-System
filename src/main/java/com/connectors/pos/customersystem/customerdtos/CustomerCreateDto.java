package com.connectors.pos.customersystem.customerdtos;

import com.connectors.pos.customersystem.CustomerPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;


public record CustomerCreateDto(
        @NotBlank(message="{validation.customer.name.required}")
       @Size(max=50)
        String name,
        @Size(max = 1000)
        String location,
        @Size(max = 50)
        String shippingCompany,
        List<@Size(max = 50) String> phoneNumbers


) {
}
