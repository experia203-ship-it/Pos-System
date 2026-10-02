package com.connectors.pos.purchasesystem.purchasedtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorCreateDto(
        Long id,
        @NotBlank(message = "{validation.vendor.name.required}")
        @Size(max = 255)
        String name,
        @Size(max = 500)
        String location,
        @Size(max = 50)
        String phoneNumber,
        @Size(max = 50)
        String landline
) {
}
