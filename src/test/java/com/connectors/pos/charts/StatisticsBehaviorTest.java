package com.connectors.pos.charts;

import com.connectors.pos.ordersystem.OrderItemRepository;
import com.connectors.pos.ordersystem.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatisticsBehaviorTest {

    @Test
    void aggregateStatsUseInclusiveFullDayBoundsAndPreserveMonetaryValues() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderItemRepository items = mock(OrderItemRepository.class);
        LocalDate start = LocalDate.of(2026, 4, 3);
        LocalDate end = LocalDate.of(2026, 4, 7);
        when(orders.calculateTotalSales(start.atStartOfDay(), end.atTime(LocalTime.MAX)))
                .thenReturn(new BigDecimal("1234.50"));
        when(orders.calculateRevenueBetweenDates(start.atStartOfDay(), end.atTime(LocalTime.MAX)))
                .thenReturn(new BigDecimal("234.75"));

        Statistics stats = new StatisticsService(orders, items).calculateStats(start, end);

        assertEquals(new BigDecimal("1234.50"), stats.totalSales());
        assertEquals(new BigDecimal("234.75"), stats.totalProfit());
        assertNotNull(stats.reportDate());
        verify(orders).calculateTotalSales(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        verify(orders).calculateRevenueBetweenDates(start.atStartOfDay(), end.atTime(LocalTime.MAX));
    }

    @Test
    void rankedItemQueriesForwardDateBoundsAndRequestedPage() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderItemRepository items = mock(OrderItemRepository.class);
        StatisticsService service = new StatisticsService(orders, items);
        LocalDate date = LocalDate.of(2026, 5, 12);
        PageRequest page = PageRequest.of(0, 4);
        List<TopSellingItemDto> top = List.of(new TopSellingItemDto("Filter", 9L));
        List<WorstSellingItemDto> least = List.of(new WorstSellingItemDto("Mirror", 1L));
        when(items.getTopSellingItems(date.atStartOfDay(), date.atTime(LocalTime.MAX), page)).thenReturn(top);
        when(items.getLeastSellingItems(date.atStartOfDay(), date.atTime(LocalTime.MAX), page)).thenReturn(least);

        assertEquals(top, service.findTopSelling(date, date, page));
        assertEquals(least, service.findLeastSelling(date, date, page));

        verify(items).getTopSellingItems(date.atStartOfDay(), date.atTime(LocalTime.MAX), page);
        verify(items).getLeastSellingItems(date.atStartOfDay(), date.atTime(LocalTime.MAX), page);
    }

    @Test
    void emptyDateRangeAggregatesAreReportedAsZero() {
        OrderRepository orders = mock(OrderRepository.class);
        OrderItemRepository items = mock(OrderItemRepository.class);
        LocalDate date = LocalDate.of(2026, 6, 4);
        when(orders.calculateTotalSales(date.atStartOfDay(), date.atTime(LocalTime.MAX))).thenReturn(null);
        when(orders.calculateRevenueBetweenDates(date.atStartOfDay(), date.atTime(LocalTime.MAX))).thenReturn(null);

        Statistics stats = new StatisticsService(orders, items).calculateStats(date, date);

        assertEquals(BigDecimal.ZERO, stats.totalSales());
        assertEquals(BigDecimal.ZERO, stats.totalProfit());
    }
}
