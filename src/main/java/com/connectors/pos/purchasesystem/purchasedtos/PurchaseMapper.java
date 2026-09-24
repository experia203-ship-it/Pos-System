package com.connectors.pos.purchasesystem.purchasedtos;

import com.connectors.pos.purchasesystem.PurchaseOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PurchaseMapper {

   @Mapping(target = "vendorName", source = "vendor.name")
   @Mapping(target = "vendorId", source = "vendor.id")
   @Mapping(target = "userName", source = "user.name")
   @Mapping(target = "userId", source = "user.id")
   PurchaseOrderResponseDto  toResponse (PurchaseOrder order);

}
