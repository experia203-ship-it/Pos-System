package com.connectors.pos.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface CashDrawerRepository extends JpaRepository<CashDrawerEvent,Long> {

@Query("select sum(coalesce(e.amount,0)) from CashDrawerEvent e where e.shiftSession.id=:id and e.eventType=:type")
    BigDecimal sumAllCashEventsDuringShift(@Param("id") Long id , @Param("type") EventType type );
}
