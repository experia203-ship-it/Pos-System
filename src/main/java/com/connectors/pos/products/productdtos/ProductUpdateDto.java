package com.connectors.pos.products.productdtos;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductUpdateDto(

        @Size(max=255 , message="you exceeded the max length allowed")
        String name,
        @Size(max=50,message="you exceeded the max length allowed")
        String partNumber,
        @Size(max=255,message="you exceeded the max length allowed")
        String description,
        @Positive(message = "selling price can't equal 0 or less than 0")
        BigDecimal sellingPrice,
        @Positive(message = "purchasing price can't equal 0 or less than 0")
        BigDecimal purchasePrice,
        @PositiveOrZero(message ="stock can't be below zero ")
        Long stock,
        Long categoryId

) {
}
