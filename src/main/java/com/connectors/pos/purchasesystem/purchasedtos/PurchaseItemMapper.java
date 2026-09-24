package com.connectors.pos.purchasesystem.purchasedtos;

import com.connectors.pos.purchasesystem.PurchaseOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PurchaseItemMapper {


    @Mapping(target = "productSellingPrice", source = "product.purchasePrice")
    @Mapping(target = "productId", source = "product.id")
    PurchaseItemDto   toResponse (PurchaseOrderItem item);
}
