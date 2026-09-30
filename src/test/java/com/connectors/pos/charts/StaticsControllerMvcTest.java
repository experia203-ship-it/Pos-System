package com.connectors.pos.charts;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class StaticsControllerMvcTest {
    private final StatisticsService service = mock(StatisticsService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new StaticsController(service)).build();

    @Test
    void customDateRangeReturnsStatisticsAndBothRankedListsUsingSeparatePageSizes() throws Exception {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        Statistics stats = new Statistics(new BigDecimal("200"), new BigDecimal("20"), LocalDateTime.now());
        List<TopSellingItemDto> top = List.of(new TopSellingItemDto("Oil", 12L));
        List<WorstSellingItemDto> least = List.of(new WorstSellingItemDto("Lamp", 1L));
        when(service.calculateStats(start, end)).thenReturn(stats);
        when(service.findTopSelling(start, end, PageRequest.of(0, 5))).thenReturn(top);
        when(service.findLeastSelling(start, end, PageRequest.of(0, 2))).thenReturn(least);

        mvc.perform(get("/statics/custom").param("dateRange", " 2026-02-01 to 2026-02-28 ")
                        .param("size", "5").param("leastSize", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/stat :: stat-res"))
                .andExpect(model().attribute("res", stats))
                .andExpect(model().attribute("top", top))
                .andExpect(model().attribute("least", least));

        verify(service).calculateStats(start, end);
        verify(service).findTopSelling(start, end, PageRequest.of(0, 5));
        verify(service).findLeastSelling(start, end, PageRequest.of(0, 2));
    }

    @Test
    void singleDateIsTreatedAsBothRangeEndpoints() throws Exception {
        LocalDate date = LocalDate.of(2026, 3, 8);
        mvc.perform(get("/statics/custom").param("dateRange", "2026-03-08")
                        .param("size", "10").param("leastSize", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/stat :: stat-res"));

        verify(service).calculateStats(date, date);
        verify(service).findTopSelling(date, date, PageRequest.of(0, 10));
        verify(service).findLeastSelling(date, date, PageRequest.of(0, 10));
    }

    @Test
    void invalidDateIsRejectedRatherThanQueried() throws Exception {
        mvc.perform(get("/statics/custom").param("dateRange", "not-a-date")
                        .param("size", "10").param("leastSize", "10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("res", org.hamcrest.Matchers.nullValue()));
        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    void reversedRangesAndOversizedListsAreRejectedWithoutQuerying() throws Exception {
        mvc.perform(get("/statics/custom").param("dateRange", "2026-06-05 to 2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("res", org.hamcrest.Matchers.nullValue()));

        mvc.perform(get("/statics/custom").param("dateRange", "2026-06-05")
                        .param("size", "51").param("leastSize", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("res", org.hamcrest.Matchers.nullValue()));

        org.mockito.Mockito.verifyNoInteractions(service);
    }
}
