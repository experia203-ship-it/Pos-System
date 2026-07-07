package com.connectors.pos.products.categorydtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateDto(

        @NotBlank(message="name is required")
        @Size(max=50 , message="you exceeded the number of characters allowed")

        String name,

        @Size(max=255,message="you exceeded the number of characters allowed")
        String description

) {
}
