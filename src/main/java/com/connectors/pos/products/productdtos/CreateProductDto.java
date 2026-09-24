package com.connectors.pos.products.productdtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public record CreateProductDto(

        @NotBlank(message = "name is required")
        @Size(max=255 , message="you exceeded the max length allowed")
       String name,
       @NotBlank(message = "partNumber is required")
       @Size(max=50,message="you exceeded the max length allowed")
       String partNumber,
        @NotBlank(message = "description is required")
        @Size(max=255,message="you exceeded the max length allowed")
       String description,
      @NotNull(message = "selling price is required")
      @Positive(message = "selling price can't equal 0 or less than 0")
       BigDecimal sellingPrice,
        @NotNull(message = "purchase price is required")
        @Positive(message = "purchasing price can't equal 0 or less than 0")
       BigDecimal purchasePrice,
        @NotNull(message = "stock price is required")
        @PositiveOrZero(message ="stock can't be below zero ")
       Long stock,
        @NotNull(message = "choose an existing category or create a new one")
       Long categoryId,
        @Size(max=50)
        String barcode,
        @PositiveOrZero(message="orderPoint can't be below zero")
        Long reorderPoint,

        Map<String,String> customFields


) {

    public CreateProductDto{
        if(customFields==null){
            customFields=new HashMap<>();
        }

    }
}
