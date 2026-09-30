package com.connectors.pos.charts;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.time.format.DateTimeParseException;

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
    public String getStatisticsByDate(@RequestParam(required = false) String dateRange, Model model,
                                       @RequestParam(defaultValue = "5") int size,
                                       @RequestParam(defaultValue = "1") int leastSize) {
        LocalDate[] dates = parseDateRange(dateRange);
        if (dates == null || size < 1 || size > 50 || leastSize < 1 || leastSize > 50) {
            model.addAttribute("res", null);
            model.addAttribute("top", List.of());
            model.addAttribute("least", List.of());
            return "fragments/stat :: stat-res";
        }

        LocalDate start = dates[0];
        LocalDate end = dates[1];
        Statistics result = statServo.calculateStats(start, end);
        Pageable topPage = PageRequest.of(0, size);
        Pageable leastPage = PageRequest.of(0, leastSize);
        model.addAttribute("res", result);
        model.addAttribute("top", statServo.findTopSelling(start, end, topPage));
        model.addAttribute("least", statServo.findLeastSelling(start, end, leastPage));
        return "fragments/stat :: stat-res";
    }

    private static LocalDate[] parseDateRange(String dateRange) {
        if (dateRange == null || dateRange.isBlank()) {
            return null;
        }
        String[] parts = dateRange.trim().split("\\s+to\\s+", -1);
        if (parts.length == 1) {
            parts = new String[]{parts[0], parts[0]};
        }
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return null;
        }
        try {
            LocalDate start = LocalDate.parse(parts[0].trim());
            LocalDate end = LocalDate.parse(parts[1].trim());
            return start.isAfter(end) ? null : new LocalDate[]{start, end};
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}




