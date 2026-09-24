package com.connectors.pos.ordersystem;

import com.connectors.pos.charts.TopSellingItemDto;
import com.connectors.pos.charts.WorstSellingItemDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {


    @Query("""
select new com.connectors.pos.charts.TopSellingItemDto(oi.product.name , sum(oi.quantity))
from OrderItem oi where oi.order.createdAt >= :start and oi.order.createdAt<= :end
group by oi.product.id , oi.product.name
order by sum(oi.quantity) desc
""")
    List<TopSellingItemDto> getTopSellingItems(@Param("start")LocalDateTime start, @Param("end") LocalDateTime end , Pageable pageable);

    @Query("""
            select new com.connectors.pos.charts.WorstSellingItemDto(oi.product.name,sum(oi.quantity))
            from OrderItem oi where oi.order.createdAt >= :start and oi.order.createdAt <= :end
            group by oi.product.id , oi.product.name
                        order by sum(oi.quantity) asc
            """)
    List<WorstSellingItemDto> getLeastSellingItems(@Param("start") LocalDateTime start,@Param("end") LocalDateTime end,Pageable pageable);
}
