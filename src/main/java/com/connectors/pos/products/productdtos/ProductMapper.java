package com.connectors.pos.products.productdtos;

import com.connectors.pos.products.Products;
import org.mapstruct.*;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMapper {



     @Mapping(target = "active", ignore = true)
     @Mapping(target = "updatedAt", ignore = true)
     @Mapping(target = "id", ignore = true)
     @Mapping(target = "createdAt", ignore = true)
     @Mapping(target = "category", ignore = true)
     @Mapping(target = "barcode", ignore = true)

     Products toEntity(CreateProductDto dto);


        @Mapping(target = "categoryId", source="category.id")
        ProductResponseDto toResponse(Products product);


        @Mapping(target = "updatedAt", ignore = true)
        @Mapping(target = "active", ignore = true)
        @Mapping(target = "id", ignore = true)
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "category", ignore=true)
        @Mapping(target = "barcode", ignore=true)

        @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
        void updateEntityFromDto(ProductUpdateDto dto, @MappingTarget Products product);





        // 1. Tells MapStruct how to convert Entity (Object) to DTO (String)
        default Map<String, String> mapObjectToString(Map<String, Object> customFields) {
            if (customFields == null) {
                return new HashMap<>();
            }

            Map<String, String> stringMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : customFields.entrySet()) {
                // Safely convert whatever the Object is into a String
                String stringValue = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
                stringMap.put(entry.getKey(), stringValue);
            }
            return stringMap;
        }

        // 2. Tells MapStruct how to convert DTO (String) to Entity (Object)
        default Map<String, Object> mapStringToObject(Map<String, String> customFields) {
            if (customFields == null) {
                return new HashMap<>();
            }

            // A String is already an Object, so we can just pass the map directly
            return new HashMap<>(customFields);
        }
    }


