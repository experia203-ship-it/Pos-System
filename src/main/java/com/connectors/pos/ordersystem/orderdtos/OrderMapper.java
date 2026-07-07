package com.connectors.pos.ordersystem.orderdtos;

import com.connectors.pos.ordersystem.Order;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.ERROR,uses = OrderItemMapper.class
, nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface OrderMapper {

     @Mapping(target = "orderNumber", ignore = true)
     @Mapping(target = "userName", ignore = true)
     @Mapping(target = "user", ignore = true)
     @Mapping(target = "total", ignore = true)
     @Mapping(target = "revenue", ignore = true)
     @Mapping(target = "remaining", ignore = true)
     @Mapping(target = "orderItems", ignore = true)
     @Mapping(target = "id", ignore = true)
     @Mapping(target = "customer", ignore = true)
     @Mapping(target = "createdAt", ignore = true)
     Order toEntity(OrderCreateDto dto);


    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target="customerName",source = "customer.name")
    OrderResponseDto toResponse (Order order);


         List<OrderResponseDto> toListResponse(Iterable<Order> orders);

}
