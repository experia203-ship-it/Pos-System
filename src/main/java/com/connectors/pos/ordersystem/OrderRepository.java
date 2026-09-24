package com.connectors.pos.ordersystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {


    boolean existsByOrderNumber(String orderNumber);



    List<Order> findByOrderNumber(String orderNumber);

   @Query(value="select nextval('order_number_seq')",nativeQuery = true)
    Long getNextOrderSequence();

   @Query("select max(o.orderNumber) from Order o")
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

@Query("select coalesce(sum(o.total),0),coalesce(sum(o.paid),0),coalesce(sum(o.remaining),0) "+
"from Order o where o.customer.id = :id and o.createdAt between :start and :end")
Object[] sumAllOrdersSummaryBetweenDatesById(@Param("id") Long id,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);


@Query("select o from Order o where o.customer.id =:id and o.createdAt between :start and :end")
   Page<Order> findOrdersByCustomerIdBetweenDates(@Param("id") Long id , @Param("start") LocalDateTime start,@Param("end") LocalDateTime end,Pageable pageable);


@Query("select sum(coalesce(o.paid,0)) from Order o where o.shiftSession.id=:id ")
    BigDecimal sumShiftTotal(@Param("id") Long shiftId);

}
