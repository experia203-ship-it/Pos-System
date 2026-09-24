package com.connectors.pos.charts;

import com.connectors.pos.ordersystem.OrderItemRepository;
import com.connectors.pos.ordersystem.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.module.ModuleDescriptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;


    public Statistics calculateStats(LocalDate start, LocalDate end) {

        BigDecimal totalSales = countTotalSales(start, end);
        BigDecimal totalRevenue = countTotalRevenue(start, end);

        return new Statistics(totalSales, totalRevenue, LocalDateTime.now());

    }

    @Transactional(readOnly = true)
    private BigDecimal countTotalSales(LocalDate start, LocalDate end) {

        LocalDateTime startTime = start.atStartOfDay();

        LocalDateTime endTime = end.atTime(LocalTime.MAX);

        return orderRepo.calculateTotalSales(startTime, endTime);


    }

    @Transactional(readOnly = true)
    private BigDecimal countTotalRevenue(LocalDate start, LocalDate end) {
        LocalDateTime startTime = start.atStartOfDay();

        LocalDateTime endTime = end.atTime(LocalTime.MAX);

        return orderRepo.calculateRevenueBetweenDates(startTime, endTime);

    }

    @Transactional(readOnly = true)

    public List<TopSellingItemDto> findTopSelling(LocalDate start, LocalDate end, Pageable pageable) {

        LocalDateTime st = start.atStartOfDay();
        LocalDateTime ed = end.atTime(LocalTime.MAX);

        List<TopSellingItemDto> result = orderItemRepo.getTopSellingItems(st, ed, pageable);


        return result;


    }

    @Transactional(readOnly = true)
    public List<WorstSellingItemDto> findLeastSelling(LocalDate start,LocalDate end , Pageable pageable){
        LocalDateTime st = start.atStartOfDay();
        LocalDateTime ed = end.atTime(LocalTime.MAX);

        List<WorstSellingItemDto> result = orderItemRepo.getLeastSellingItems(st,ed,pageable);

        return result;

    }

}
