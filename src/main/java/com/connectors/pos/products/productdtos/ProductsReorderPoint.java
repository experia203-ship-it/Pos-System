package com.connectors.pos.products.productdtos;

import org.springframework.data.domain.Page;

public record ProductsReorderPoint(

        Page<ProductResponseDto> products,
        Long count


) {
}
