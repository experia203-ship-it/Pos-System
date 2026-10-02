package com.connectors.pos.ordersystem;
import com.connectors.pos.ordersystem.orderdtos.OrderTotals;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findForUpdateById(@Param("id") Long id);

    boolean existsByOrderNumber(String orderNumber);



    List<Order> findByOrderNumber(String orderNumber);

   @Query("select max(cast(o.orderNumber as long)) from Order o")
Long findMaxOrderNumber();

@Query("select sum(o.revenue) from Order o where o.createdAt >= :start and o.createdAt <= :end")
BigDecimal calculateRevenueBetweenDates(@Param("start") LocalDateTime start , @Param("end") LocalDateTime end);

@Query("select sum(o.total) from Order o where o.createdAt >= :start and o.createdAt <= :end ")
BigDecimal calculateTotalSales(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

@Query("select o from Order o where lower(o.customer.name) like lower(concat('%',:keyword,'%'))")
Page<Order> findOrdersByCustomerNameContainingKeyword(@Param("keyword") String keyword,Pageable pageable);

@Query("select o from Order o where lower(o.customer.name) like lower(concat('%',:keyword,'%')) and" +
     " o.createdAt between :start and :end"   )
Page<Order> findOrdersByCustomerNameBetweenDates(@Param("keyword") String keyword,@Param("start") LocalDateTime start , @Param("end") LocalDateTime end,Pageable pageable);

@Query("select new com.connectors.pos.ordersystem.orderdtos.OrderTotals("
        + "coalesce(sum(o.total - o.returnCredit),0),"
        + "coalesce(sum(o.paid - o.refundedTotal),0),"
        + "coalesce(sum(case when o.total > o.paid + o.returnCredit "
        + "then o.total - o.paid - o.returnCredit else 0 end),0)) "+
"from Order o where o.customer.id = :id and o.createdAt between :start and :end")
OrderTotals sumAllOrdersSummaryBetweenDatesById(@Param("id") Long id, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


@Query("select o from Order o where o.customer.id =:id and o.createdAt between :start and :end")
   Page<Order> findOrdersByCustomerIdBetweenDates(@Param("id") Long id , @Param("start") LocalDateTime start,@Param("end") LocalDateTime end,Pageable pageable);


@Query("select sum(case when o.paymentMethod = com.connectors.pos.ordersystem.PaymentMethod.CASH "
        + "then coalesce(o.paid,0) else 0 end) "
        + "from Order o where o.shiftSession.id=:id")
    BigDecimal sumShiftCashPayments(@Param("id") Long shiftId);

}
