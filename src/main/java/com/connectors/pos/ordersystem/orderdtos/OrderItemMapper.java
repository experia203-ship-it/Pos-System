package com.connectors.pos.ordersystem.orderdtos;

import com.connectors.pos.ordersystem.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderItemMapper {





        @Mapping(target = "productId", source="product.id")
        OrderItemResponseDto toResponse (OrderItem item);


}
