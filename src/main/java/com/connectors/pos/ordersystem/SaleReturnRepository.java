package com.connectors.pos.ordersystem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    @Query("select coalesce(sum(i.quantity), 0) from SaleReturnItem i where i.orderItem.id = :itemId")
    long sumReturnedQuantity(@Param("itemId") Long itemId);

    @Query("select coalesce(sum(i.creditTotal), 0) from SaleReturnItem i where i.orderItem.id = :itemId")
    BigDecimal sumReturnedCredit(@Param("itemId") Long itemId);

    @Query("select coalesce(sum(r.creditTotal), 0) from SaleReturn r where r.order.id = :orderId")
    BigDecimal sumOrderCredits(@Param("orderId") Long orderId);

    @Query("select coalesce(sum(r.refundTotal), 0) from SaleReturn r where r.order.id = :orderId")
    BigDecimal sumOrderRefunds(@Param("orderId") Long orderId);

    @Query("select coalesce(sum(r.refundTotal), 0) from SaleReturn r "
            + "where r.shiftSession.id = :shiftId and r.paymentMethod = com.connectors.pos.ordersystem.PaymentMethod.CASH")
    BigDecimal sumCashRefundsForShift(@Param("shiftId") Long shiftId);

    List<SaleReturn> findByOrder_IdOrderByCreatedAtDesc(Long orderId);

    boolean existsByOrder_Id(Long orderId);
}
