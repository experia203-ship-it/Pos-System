package com.connectors.pos.charts;

import com.connectors.pos.ordersystem.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class StatisticsService {
  private final OrderRepository orderRepo;



  public Statistics calculateStats(LocalDate start,LocalDate end){

      BigDecimal totalSales = countTotalSales(start , end);
      BigDecimal totalRevenue = countTotalRevenue(start,end);

      Statistics stats = new Statistics(totalSales,totalRevenue,LocalDateTime.now());

      return stats;
  }
  @Transactional(readOnly = true)
  private BigDecimal countTotalSales(LocalDate start , LocalDate end){

      LocalDateTime startTime = start.atStartOfDay();

      LocalDateTime endTime = end.atTime(LocalTime.MAX);

      BigDecimal result = orderRepo.calculateTotalSales(startTime,endTime);

      return result;

  }

    @Transactional(readOnly = true)
    private BigDecimal countTotalRevenue(LocalDate start,LocalDate end){
      LocalDateTime startTime = start.atStartOfDay();

      LocalDateTime endTime = end.atTime(LocalTime.MAX);

      BigDecimal result = orderRepo.calculateRevenueBetweenDates(startTime,endTime);

      return result;

  }
}
