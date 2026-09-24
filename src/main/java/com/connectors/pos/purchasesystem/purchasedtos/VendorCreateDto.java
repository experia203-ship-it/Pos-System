package com.connectors.pos.purchasesystem.purchasedtos;

public record VendorCreateDto(
        Long id,
        String name,
        String location,
        String phoneNumber,
        String landline
) {
}
