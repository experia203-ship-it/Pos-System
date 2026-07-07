package com.connectors.pos.products.categorydtos;

import com.connectors.pos.products.Categories;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryMapper {

   @Mapping(target = "active", ignore = true)
   @Mapping(target = "products", ignore = true)
   @Mapping(target = "id", ignore = true)
   Categories toEntity(CategoryCreateDto dto);

      CategoryResponseDto toResponse(Categories categories);

       List<CategoryResponseDto> toDtoResponse(List<Categories> categories);

}
