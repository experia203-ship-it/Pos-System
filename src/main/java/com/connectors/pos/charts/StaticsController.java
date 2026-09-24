package com.connectors.pos.charts;

import lombok.RequiredArgsConstructor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/statics")
public class StaticsController {
    private final StatisticsService statServo;

    @GetMapping
    public String viewStatisticsPage() {


        return "fragments/statics";
    }


    @GetMapping("/custom")
    public String getStatisticsByDate(@RequestParam String dateRange, Model model , @RequestParam int size , @RequestParam int leastSize) {
        String startDate = null;
        String endDate = null;

        if (dateRange.contains(" to ")) {
            String[] dates = dateRange.split(" to ");
            if (dates.length == 2) {
                startDate = dates[0].trim();
                endDate = dates[1].trim();
            }
        } else {
            startDate = dateRange.trim();
            endDate = startDate;

        }
        if ((startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
            LocalDate fixedStart = LocalDate.parse(startDate);
            LocalDate fixedEnd = LocalDate.parse(endDate);

            Statistics res = statServo.calculateStats(fixedStart, fixedEnd);
            model.addAttribute("res", res);

            Pageable pageable = PageRequest.of(0,size);
            Pageable leastPageable = PageRequest.of(0,leastSize);
            List<TopSellingItemDto> top = statServo.findTopSelling(fixedStart,fixedEnd,pageable);

            model.addAttribute("top",top);

            List<WorstSellingItemDto> least = statServo.findLeastSelling(fixedStart,fixedEnd,leastPageable);
            model.addAttribute("least",least);
        }
        return "fragments/stat :: stat-res";

    }
}





