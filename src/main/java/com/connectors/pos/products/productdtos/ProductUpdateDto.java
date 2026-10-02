package com.connectors.pos.products.productdtos;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public record ProductUpdateDto(

        @Size(max=255 , message="{validation.common.maxLength.exceeded}")
        String name,
        @Size(max=50,message="{validation.common.maxLength.exceeded}")
        String partNumber,
        @Size(max=255,message="{validation.common.maxLength.exceeded}")
        String description,
        @Positive(message = "{validation.common.positive.sellingPrice}")
        BigDecimal sellingPrice,
        @Positive(message = "{validation.common.positive.purchasePrice}")
        BigDecimal purchasePrice,
        @PositiveOrZero(message ="{validation.common.positiveOrZero.stock}")
        Long stock,
        Long categoryId,
        @Size(max=50,message="{validation.common.maxLength.exceeded}")
        String barcode,
        @PositiveOrZero(message="{validation.common.positiveOrZero.reorderPoint}")
        Long reorderPoint,
        Map<String,String> customFields


) {

        public  ProductUpdateDto{

                if(customFields==null){

                        customFields = new HashMap<>();
                }


        }
}
