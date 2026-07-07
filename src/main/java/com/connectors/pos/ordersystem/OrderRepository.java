package com.connectors.pos.ordersystem;
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

@Query("select sum(o.revenue) from Order o where o.createdAt >= :start and o.createdAt <= :end")
BigDecimal calculateRevenueBetweenDates(@Param("start") LocalDateTime start , @Param("end") LocalDateTime end);

@Query("select sum(o.total) from Order o where o.createdAt >= :start and o.createdAt <= :end ")
BigDecimal calculateTotalSales(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


}
