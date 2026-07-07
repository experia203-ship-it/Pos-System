package com.connectors.pos.products.productdtos;

import com.connectors.pos.products.Products;
import org.mapstruct.*;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMapper {



     @Mapping(target = "active", ignore = true)
     @Mapping(target = "updatedAt", ignore = true)
     @Mapping(target = "id", ignore = true)
     @Mapping(target = "createdAt", ignore = true)
     @Mapping(target = "category", ignore = true)
     Products toEntity(CreateProductDto dto);


        @Mapping(target = "categoryId", source="category.id")
        ProductResponseDto toResponse(Products product);


        @Mapping(target = "updatedAt", ignore = true)
        @Mapping(target = "active", ignore = true)
        @Mapping(target = "id", ignore = true)
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "category", ignore=true)
        @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
        void updateEntityFromDto(ProductUpdateDto dto, @MappingTarget Products product);
}
