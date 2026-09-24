package com.connectors.pos.customersystem.customerdtos;

import com.connectors.pos.customersystem.CustomerPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;


public record CustomerCreateDto(
        @NotBlank(message="customer name is required")
       @Size(max=50)
        String name,
         String location,
        String shippingCompany,
        List<String> phoneNumbers


) {
}
