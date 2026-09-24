package com.connectors.pos.customersystem.customerdtos;

public record CustomerViewDto(
        Long id ,
        String name,
        String location,
        String shippingCompany,
        String phoneNumber
) {
}
