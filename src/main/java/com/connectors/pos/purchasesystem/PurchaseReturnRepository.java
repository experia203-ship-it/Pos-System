package com.connectors.pos.purchasesystem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    @Query("select coalesce(sum(i.quantity), 0) from PurchaseReturnItem i "
            + "where i.purchaseOrderItem.id = :purchaseItemId")
    long sumReturnedQuantity(@Param("purchaseItemId") Long purchaseItemId);

    @Query("select coalesce(sum(i.creditTotal), 0) from PurchaseReturnItem i "
            + "where i.purchaseOrderItem.id = :purchaseItemId")
    BigDecimal sumReturnedCredit(@Param("purchaseItemId") Long purchaseItemId);
}
