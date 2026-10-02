package com.connectors.pos.products.productdtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public record CreateProductDto(

        @NotBlank(message = "{validation.common.name.required}")
        @Size(max=255 , message="{validation.common.maxLength.exceeded}")
       String name,
       @NotBlank(message = "{validation.product.partNumber.required}")
       @Size(max=50,message="{validation.common.maxLength.exceeded}")
       String partNumber,
        @NotBlank(message = "{validation.product.description.required}")
        @Size(max=255,message="{validation.common.maxLength.exceeded}")
       String description,
      @NotNull(message = "{validation.product.sellingPrice.required}")
      @Positive(message = "{validation.common.positive.sellingPrice}")
       BigDecimal sellingPrice,
        @NotNull(message = "{validation.product.purchasePrice.required}")
        @Positive(message = "{validation.common.positive.purchasePrice}")
       BigDecimal purchasePrice,
        @NotNull(message = "{validation.product.stock.required}")
        @PositiveOrZero(message ="{validation.common.positiveOrZero.stock}")
       Long stock,
        @NotNull(message = "{validation.product.category.required}")
       Long categoryId,
        @Size(max=50)
        String barcode,
        @PositiveOrZero(message="{validation.common.positiveOrZero.reorderPoint}")
        Long reorderPoint,

        Map<String,String> customFields


) {

    public CreateProductDto{
        if(customFields==null){
            customFields=new HashMap<>();
        }

    }
}
