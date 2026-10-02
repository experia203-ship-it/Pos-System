package com.connectors.pos.products.categorydtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateDto(

        @NotBlank(message="{validation.common.name.required}")
        @Size(max=50 , message="{validation.common.maxLength.exceeded}")

        String name,

        @Size(max=255,message="{validation.common.maxLength.exceeded}")
        String description

) {
}
